#!/usr/bin/env python3
"""
HTTP-driven embedding evaluation for klibs.io.

Drives an ALREADY-RUNNING app instance (default http://localhost:8080) instead of
booting a second copy. For each concept query it fetches rankings from every engine
(FTS baseline + one semantic engine per embedding column), pools the candidates,
grades them 0-3 with an LLM judge, then computes nDCG@10 / MRR / Recall@10 / Recall@20
and search-latency stats (avg / max / p50 / p95) per engine.

Pure Python stdlib; needs only PERSONAL_AI_TOKEN in the environment for the judge.
"""
import json
import math
import os
import statistics
import sys
import time
import urllib.request
import urllib.parse
import urllib.error

BASE_URL = os.environ.get("KLIBS_BASE_URL", "http://localhost:8080")
OPENAI_KEY = os.environ.get("PERSONAL_AI_TOKEN", "")
JUDGE_MODEL = os.environ.get("KLIBS_JUDGE_MODEL", "gpt-4o-mini")
TOP_N = int(os.environ.get("KLIBS_EVAL_TOP_N", "150"))
LIMIT = int(os.environ.get("KLIBS_EVAL_LIMIT", "20"))
REL_THRESHOLD = 2  # grade >= 2 counts as relevant for Recall/MRR

QUERIES_FILE = os.environ.get("KLIBS_QUERIES_FILE", "app/src/main/resources/eval/concept-queries.txt")
OUT_DIR = "eval-output"
REPORT_FILE = os.environ.get("KLIBS_REPORT_FILE", os.path.join(OUT_DIR, "embedding-eval-report.md"))
JUDGMENTS_FILE = os.environ.get("KLIBS_JUDGMENTS_FILE", os.path.join(OUT_DIR, "judgments-http.json"))

SEMANTIC_ENGINES = ["openai-3-small", "openai-3-large", "openai-ada-002", "bge-large-local"]
ENGINES = ["fts"] + SEMANTIC_ENGINES

JUDGE_SYSTEM = (
    "You grade search results for klibs.io, a catalog of Kotlin Multiplatform libraries.\n"
    "For each candidate project decide how well it satisfies the developer's search query intent.\n"
    "Grades: 0 = irrelevant, 1 = marginally related, 2 = relevant, 3 = a perfect match.\n"
    "Judge by capability/topic, not by name similarity. Return a grade for every candidate id.\n"
    'Return ONLY JSON of the form {"grades":[{"id":<int>,"grade":<0-3>}]}.'
)


def log(msg):
    print(f"[{time.strftime('%H:%M:%S')}] {msg}", flush=True)


def load_queries():
    qs = []
    with open(QUERIES_FILE, encoding="utf-8") as f:
        for line in f:
            line = line.rstrip("\n")
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            q = parts[0].strip()
            if q:
                qs.append(q)
            if len(qs) >= TOP_N:
                break
    return qs


def http_post_json(url, body, timeout):
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def http_get_json(url, timeout):
    req = urllib.request.Request(url, method="GET")
    with urllib.request.urlopen(req, timeout=timeout) as resp:
        return json.loads(resp.read().decode("utf-8"))


def fetch_ranking(engine, query):
    """Returns (results_list, latency_ms). results_list = list of project dicts in rank order."""
    start = time.perf_counter()
    if engine == "fts":
        url = f"{BASE_URL}/search/projects?query={urllib.parse.quote(query)}&limit={LIMIT}"
        res = http_get_json(url, timeout=60)
    else:
        url = f"{BASE_URL}/search/projects/similar?limit={LIMIT}"
        res = http_post_json(url, {"query": query, "embedder": engine}, timeout=120)
    latency_ms = (time.perf_counter() - start) * 1000.0
    return res, latency_ms


def judge_query(query, candidates):
    """candidates: list of project dicts. Returns {id: grade}."""
    if not candidates:
        return {}
    lines = [f"QUERY: {query}", "", "CANDIDATE PROJECTS (grade each by project id):"]
    for c in candidates:
        tags = ", ".join(c.get("tags") or [])
        desc = (c.get("description") or "")[:300]
        lines.append(f"- id={c['id']} | name={c.get('name')} | owner={c.get('ownerLogin')} | tags=[{tags}] | {desc}")
    user_msg = "\n".join(lines)
    body = {
        "model": JUDGE_MODEL,
        "messages": [
            {"role": "system", "content": JUDGE_SYSTEM},
            {"role": "user", "content": user_msg},
        ],
        "temperature": 0,
        "response_format": {"type": "json_object"},
    }
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        "https://api.openai.com/v1/chat/completions",
        data=data,
        headers={"Content-Type": "application/json", "Authorization": f"Bearer {OPENAI_KEY}"},
        method="POST",
    )
    for attempt in range(4):
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                payload = json.loads(resp.read().decode("utf-8"))
            content = payload["choices"][0]["message"]["content"]
            parsed = json.loads(content)
            graded = {int(g["id"]): max(0, min(3, int(g["grade"]))) for g in parsed.get("grades", [])}
            return {c["id"]: graded.get(c["id"], 0) for c in candidates}
        except urllib.error.HTTPError as e:
            wait = 5 * (attempt + 1)
            log(f"  judge HTTP {e.code}, retry in {wait}s")
            time.sleep(wait)
        except Exception as e:  # noqa
            log(f"  judge error: {e}; retry")
            time.sleep(5)
    log("  judge FAILED after retries; grading all 0")
    return {c["id"]: 0 for c in candidates}


# ---- metrics ----
def dcg(gains):
    return sum(g / math.log2(i + 2) for i, g in enumerate(gains))


def ndcg_at_k(ranked_ids, grades, k):
    gains = [(2 ** grades.get(i, 0) - 1) for i in ranked_ids[:k]]
    ideal = sorted((2 ** g - 1 for g in grades.values()), reverse=True)[:k]
    idcg = dcg(ideal)
    return dcg(gains) / idcg if idcg > 0 else 0.0


def mrr(ranked_ids, grades):
    for i, pid in enumerate(ranked_ids):
        if grades.get(pid, 0) >= REL_THRESHOLD:
            return 1.0 / (i + 1)
    return 0.0


def recall_at_k(ranked_ids, grades, k, total_relevant):
    if total_relevant == 0:
        return None  # undefined; excluded from the mean
    hit = sum(1 for pid in ranked_ids[:k] if grades.get(pid, 0) >= REL_THRESHOLD)
    return hit / total_relevant


def percentile(values, p):
    if not values:
        return 0.0
    s = sorted(values)
    rank = math.ceil(p / 100.0 * len(s))
    rank = max(1, min(rank, len(s)))
    return s[rank - 1]


def mean(xs):
    xs = [x for x in xs if x is not None]
    return sum(xs) / len(xs) if xs else 0.0


def main():
    if not OPENAI_KEY:
        log("ERROR: PERSONAL_AI_TOKEN not set")
        sys.exit(1)
    os.makedirs(OUT_DIR, exist_ok=True)
    queries = load_queries()
    log(f"Loaded {len(queries)} queries (top {TOP_N}); engines={ENGINES}")

    judgments = {}
    if os.path.exists(JUDGMENTS_FILE):
        with open(JUDGMENTS_FILE, encoding="utf-8") as f:
            judgments = json.load(f)
        log(f"Loaded {len(judgments)} cached judgments")

    # Warm up each engine once (loads bge model, primes caches) - not timed.
    log("Warming up engines...")
    for e in ENGINES:
        try:
            fetch_ranking(e, "warmup")
        except Exception as ex:  # noqa
            log(f"  warmup {e} failed: {ex}")

    # per-engine accumulators
    per_engine = {e: {"ndcg10": [], "mrr": [], "recall10": [], "recall20": [], "latency": []} for e in ENGINES}

    for qi, query in enumerate(queries, 1):
        rankings = {}
        for e in ENGINES:
            try:
                res, lat = fetch_ranking(e, query)
                rankings[e] = [r["id"] for r in res]
                per_engine[e]["latency"].append(lat)
                # collect candidate metadata
                for r in res:
                    cand_meta[r["id"]] = r
            except Exception as ex:  # noqa
                log(f"  [{qi}/{len(queries)}] {e} '{query}' failed: {ex}")
                rankings[e] = []

        # pool candidates for judging
        pool_ids = []
        seen = set()
        for e in ENGINES:
            for pid in rankings[e]:
                if pid not in seen:
                    seen.add(pid)
                    pool_ids.append(pid)
        candidates = [cand_meta[pid] for pid in pool_ids if pid in cand_meta]

        # judge (cached)
        if query in judgments:
            grades = {int(k): v for k, v in judgments[query].items()}
            # judge any new candidates not covered by cache
            missing = [c for c in candidates if c["id"] not in grades]
            if missing:
                grades.update(judge_query(query, missing))
                judgments[query] = {str(k): v for k, v in grades.items()}
        else:
            grades = judge_query(query, candidates)
            judgments[query] = {str(k): v for k, v in grades.items()}

        with open(JUDGMENTS_FILE, "w", encoding="utf-8") as f:
            json.dump(judgments, f, ensure_ascii=False, indent=0)

        total_relevant = sum(1 for g in grades.values() if g >= REL_THRESHOLD)

        for e in ENGINES:
            r = rankings[e]
            per_engine[e]["ndcg10"].append(ndcg_at_k(r, grades, 10))
            per_engine[e]["mrr"].append(mrr(r, grades))
            per_engine[e]["recall10"].append(recall_at_k(r, grades, 10, total_relevant))
            per_engine[e]["recall20"].append(recall_at_k(r, grades, 20, total_relevant))

        if qi % 10 == 0 or qi == len(queries):
            log(f"  progress {qi}/{len(queries)} (last query='{query}', relevant={total_relevant})")

    # ---- report ----
    rows = []
    for e in ENGINES:
        d = per_engine[e]
        lat = d["latency"]
        rows.append({
            "engine": e,
            "queries": len(d["ndcg10"]),
            "ndcg10": mean(d["ndcg10"]),
            "mrr": mean(d["mrr"]),
            "recall10": mean(d["recall10"]),
            "recall20": mean(d["recall20"]),
            "avg_ms": (sum(lat) / len(lat)) if lat else 0.0,
            "max_ms": max(lat) if lat else 0.0,
            "p50_ms": percentile(lat, 50),
            "p95_ms": percentile(lat, 95),
        })

    lines = []
    lines.append("# klibs.io embedding evaluation results")
    lines.append("")
    lines.append(f"- Queries: top {len(queries)} concept queries by real-log frequency")
    lines.append(f"- Candidates per engine: top {LIMIT}; pooled union judged by `{JUDGE_MODEL}` (0-3)")
    lines.append(f"- Relevant = grade >= {REL_THRESHOLD}; Recall is pooled recall (relative to judged pool)")
    lines.append(f"- Latency = full `/search/projects/similar` call (query embedding + pgvector scan), FTS = `/search/projects`")
    lines.append("")
    lines.append("| engine | queries | nDCG@10 | MRR | Recall@10 | Recall@20 | avg ms | max ms | p50 ms | p95 ms |")
    lines.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|")
    for r in rows:
        lines.append(
            f"| {r['engine']} | {r['queries']} | {r['ndcg10']:.3f} | {r['mrr']:.3f} | "
            f"{r['recall10']:.3f} | {r['recall20']:.3f} | {r['avg_ms']:.1f} | {r['max_ms']:.1f} | "
            f"{r['p50_ms']:.1f} | {r['p95_ms']:.1f} |"
        )
    report = "\n".join(lines) + "\n"
    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        f.write(report)
    log(f"Wrote report -> {REPORT_FILE}")
    print("\n" + report)


cand_meta = {}

if __name__ == "__main__":
    main()
