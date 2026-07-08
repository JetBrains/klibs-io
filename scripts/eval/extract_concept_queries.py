#!/usr/bin/env python3
"""
Extract "concept" search queries from a raw klibs.io search log.

Goal: keep queries that express a capability / need (e.g. "image picker",
"navigation", "json", "dependency injection") and drop queries that are exact
library / brand names (e.g. "ktor", "koin", "coil") as well as noise: single
letters, obvious typo prefixes, Maven coordinates and URL-encoded junk.

The result is the seed query set for the embeddings evaluation harness (E0).

Usage:
    python3 scripts/eval/extract_concept_queries.py \
        scripts/eval/raw_search_queries.txt \
        app/src/main/resources/eval/concept-queries.txt
"""
import re
import sys
import urllib.parse
from collections import Counter

# --- Exact library / brand / framework names (canonical + common typos/prefixes) ---
LIBRARIES = {
    # http clients & networking libs
    "ktor", "ktorfit", "ktorm", "ktrofit", "ktorfi", "ktorf", "ktro", "ktr", "kto",
    "fuel", "http4k", "http4", "volley", "apollo", "retrofit", "retrofi", "retrof",
    "retro", "retr", "okhttp", "okio", "ksoup", "jsoup",
    # DI
    "koin", "koil", "koi", "hilt", "dagger", "dagg", "kodein", "anvil", "metro", "katana",
    # image loading / camera
    "coil", "coil3", "kamel", "kam", "glide", "sketch", "ketch", "kache", "peekaboo",
    "imagepickerkmp", "imagepicker", "imagepick", "camerak", "kmap", "kdriver",
    # db / storage libs
    "room", "roo", "sqldelight", "sqldel", "sqlde", "exposed", "expose", "expos",
    "realm", "objectbox", "mmkv", "mmk", "kvault", "kstore", "kable", "leveldb",
    "mongo", "redis", "postgres", "mysql", "supabase", "supab", "supa", "sup",
    "firebase", "firebas", "fireba", "fireb", "fir", "fire", "firestore", "appwrite",
    # ai / cv / backend libs
    "koog", "openai", "gemini", "ollama", "llama", "tensorflow", "tenso", "tensor",
    "ten", "mediapipe", "opencv", "tesse", "hutool", "guava", "caffeine",
    # navigation / architecture libs
    "voyager", "voya", "voy", "voyag", "decompose", "decomp", "decom", "deco",
    "precompose", "circuit", "orbit", "molecule", "essenty", "appyx",
    # logging libs
    "kermit", "kermi", "napier", "napi", "nappier", "nap", "timber", "logback",
    "slf4j", "slf", "xlog", "logkat", "kdroid", "kotlin-logging",
    # date libs
    "klock", "kizitonwose", "nepali-date",
    # kotlinx / kotlin ecosystem libs
    "kotlinx", "kotlinx-io", "kotlinx.io", "kotlinx-serialization",
    "kotlinx-serialization-json", "kotlinx-serialization-core", "kotlinx.serialization",
    "kotlinx-coroutines", "kotlinx-coroutines-core", "kotlinx-coroutines-swing",
    "kotlinx.coroutines", "kotlinx.datetime", "kotlinx-datetime", "kotlinx-crypto",
    "kotlinx-ser", "kotlinx-d", "kotlinx-", "kotlinx.", "kotlin-stdlib",
    "kotlin-reflect", "kotlin-test", "kotlin-io", "kotlin-", "stdlib", "atomicfu",
    "arrow", "stately", "touchlab",
    # ios interop
    "skie", "swiftklib", "kswift", "kross",
    # graphics / ui component libs
    "korge", "korau", "skiko", "skia", "kdomskia", "jewel", "kobweb", "kuikly",
    "kuik", "kui", "ovcompose", "composemediaplayer", "compottie", "compott",
    "lottie", "lott", "lotti", "kottie", "rive", "vico", "haze", "calf", "fluent",
    "cupertino", "cupert", "cuper", "mediamp", "ijkplayer", "mpv", "vlc", "exoplayer",
    "exo", "ffmpeg", "ffmpe", "ffmp", "ffm", "unstyled", "chaintech", "alpaca",
    "smile", "confetti", "capturable", "qrose", "qrkit", "qr-kit", "kscan", "kchan",
    "ImagePickerKMP".lower(),
    # mocking / testing / build / static analysis
    "mockk", "mockito", "mokkery", "mokk", "moc", "turbine", "kotest", "junit",
    "assertk", "ultron", "kover", "jacoco", "ktlint", "detekt", "dokka", "kdoc",
    "lombok", "buildkonfig", "buildko", "buildk", "konfig", "kode", "moko",
    "moko-resources", "moko-permissions", "moko-geo", "gitlive",
    # analytics / monetization / cloud brands
    "sentry", "crashlytics", "crashl", "revenuecat", "adapty", "stripe", "strip",
    "admob", "admo", "amap", "baidu", "yandex", "mapbox", "maplibre", "mapli",
    "alipay", "wechat", "spotify", "discord", "telegram", "facebook", "microsoft",
    "github", "segment", "auth0", "kmpauth", "kmpnotifier", "msal", "cognito",
    "aws", "azure", "cloud",
    # serialization / data libs & tools
    "gson", "moshi", "jackson", "kaml", "xmlutil", "wire", "quickjs", "lua", "node",
    "chucker", "inspektor", "inspek", "kafka", "rabbitmq", "rabbit", "nats", "apache",
    "poi", "yoga", "vortex", "molecule",
    # crypto libs (algorithms kept as concepts below)
    "krypto", "kotlincrypto", "kotlinx-crypto", "openssl", "signal", "argon",
    # spring / server frameworks
    "spring", "springboot", "spring boot", "http4k",
    # networking helper libs
    "konnectivity", "konnection", "konn", "kon", "vap", "kion", "axer", "glass",
    "glas", "kache", "ktensor", "tart", "camel", "callkit", "essenty", "reorderable",
    "socketio", "modbus", "serialport", "matrix", "hls",
    # connectivity / android brand-y libs
    "accompanist", "livedata",
    # meta terms: platform / language, not a capability
    "kmp", "kmm", "klib", "klibs", "multiplatform", "multiplat", "multipla",
    "kotlin", "jetbrains", "jetbra", "androidx", "android", "androidx-navigation",
    "androidx.navigation", "ios", "apple", "darwin", "cocoa", "wasm", "desktop",
    "windows", "harmony", "platform", "plat", "native", "java", "javascript",
    "js", "rust", "python", "flutter", "react", "kuikly", "libs", "lib", "package",
    "package search", "package-search", "com", "org", "io", "kion",
}

# --- Typo / truncation fragments and generic junk that are not real concepts ---
FRAGMENTS = {
    "navi", "nav", "naviga", "navig", "navigat", "navigatio", "navigati", "navigator",
    "pag", "pagi", "pagin", "pagg", "cha", "char", "perm", "permi", "permiss",
    "permis", "permissi", "permissio", "cor", "corou", "coro", "corout", "cir",
    "dat", "datet", "dateti", "datast", "datas", "datasto", "datastor", "seri",
    "seria", "seriali", "ser", "enc", "encr", "encry", "encryp", "biom", "biome",
    "biomet", "biometr", "biometri", "calen", "cale", "calend", "calenda", "calender",
    "fing", "finger", "reor", "des", "gen", "gene", "genera", "conf", "confi", "cac",
    "cach", "loc", "loca", "logg", "loggin", "logge", "noti", "notif", "notifi",
    "notific", "notifica", "notificat", "notificatio", "med", "meida", "medi", "vide",
    "vid", "vidoe", "vedio", "que", "quick", "goog", "googl", "imag", "ima", "iamge",
    "came", "camer", "refre", "refres", "stor", "stora", "trans", "conn", "connec",
    "connecti", "depe", "depen", "depend", "depende", "dependen", "dependenc",
    "dev", "fil", "htt", "ht", "inte", "inter", "mater", "materi", "materia", "mate",
    "mat", "mar", "sca", "scann", "sett", "settin", "spl", "sto", "tim", "vie",
    "webv", "webvi", "gra", "grap", "pla", "pre", "pro", "prot", "pur", "ref",
    "res", "resou", "resour", "ret", "rou", "rout", "seri", "sha2", "corou", "netw",
    "netwo", "networ", "prefere", "prefer", "prefe", "preferen", "markd", "markdo",
    "markdow", "constr", "constra", "constrain", "compre", "compres", "capt", "captur",
    "anim", "anima", "sqli", "sqlit", "workma", "workman", "workmana", "geol",
    "loadmore", "load", "new", "about", "info", "meta", "result", "step", "steps",
    "tip", "rule", "select", "selector", "provider", "feature", "template", "size",
    "effect", "object", "collection", "properties", "annotation", "reflect",
    "functional", "reactive", "process", "thread", "job", "direct", "system",
    "startup", "environment", "config", "configuration", "context", "state", "event",
    "eventbus", "bus", "flow", "reveal", "spinner", "reader", "editor", "edit",
    "col", "gr", "vec", "po", "ba", "des", "int", "ic", "id", "ip", "op", "sa",
    "si", "so", "sp", "st", "ta", "te", "to", "tu", "ac", "al", "an", "ar", "arr",
    "au", "aud", "audi", "andr", "andro", "ali", "ada", "bl", "blu", "blue", "bluet",
    "blueto", "bluetoo", "bluetoot", "barc", "bar", "car", "ch", "ci", "cl", "clip",
    "cmp", "co", "coi", "coin", "coli", "comp", "compo", "compos", "con", "cou",
    "cre", "cry", "cryp", "crypt", "cu", "de", "dec", "dep", "dia", "do", "doc",
    "dra", "drag", "drop", "ec", "en", "es", "ev", "ex", "exce", "fe", "fi", "fil",
    "filek", "fileki", "fing", "fore", "ga", "ge", "gi", "gl", "goog", "gu", "ha",
    "hi", "hil", "htt", "ima", "im", "in", "inje", "injec", "inspe", "inter", "je",
    "jet", "ka", "ke", "ker", "keyb", "keybo", "ki", "kli", "km", "ko", "koo", "kor",
    "kot", "kr", "kry", "ks", "ku", "la", "li", "lin", "lo", "loc", "logs", "lott",
    "ma", "mac", "mater", "me", "med", "mi", "mini", "mo", "mp", "mq", "mu", "mul",
    "mv", "na", "ne", "netw", "no", "noti", "o", "ok", "op", "packa", "pac", "pack",
    "pa", "pe", "per", "ph", "phot", "pi", "pic", "pl", "post", "pu", "quick", "ra",
    "rea", "re", "ref", "reor", "res", "ret", "ri", "ro", "rou", "rx", "sc", "scann",
    "scro", "se", "sett", "shar", "sh", "ski", "sk", "sock", "socke", "sp", "spl",
    "spm", "spi", "sta", "sto", "str", "su", "subs", "sub", "sup", "swip", "sync",
    "ta", "te", "ten", "ti", "tor", "tra", "trans", "up", "util", "utils", "v",
    "vap", "vec", "vi", "vid", "vide", "vie", "viewm", "viewmo", "vo", "we", "webv",
    "wi", "wor", "yo", "zi", "genera", "gene", "gen", "num", "conf", "confi", "confie",
    "deep", "extended", "extend", "shell", "amp", "bom", "bloc", "big", "bigin",
    "bigdeci", "dec", "decimal", "float", "floating", "shadow", "toolkit", "widget",
    "widgets", "adapt", "adapti", "adaptive", "responsive", "material-icon",
    "material-icons", "material-icons-core", "material-icons-extended",
    "compose-icons", "compose-material3", "compose-navigation", "compose-cupertino",
    "compose-multiplatform", "compose-webview-multiplatform", "navigation-compose",
    "navigation3", "nav3", "constraintlayout-compose", "lifecycle-viewmodel",
    "view+model", "view model", "coil-compose", "koin-compose", "koin-core", "koin-",
    "accompanist-permissions", "accompanist-systemuicontroller", "compose-",
    "multiplatform-settings", "multiplatform-markdown-renderer", "flavor", "flavors",
    "hot reload", "reload", "review", "rate", "license", "version", "update", "save",
    "share", "call", "post", "get", "set", "put", "map", "list", "string", "text",
    "number", "color", "font", "icon", "icons", "view", "window", "screen", "tab",
    "tabs", "card", "sheet", "banner", "avatar", "badge", "slider", "switch", "step",
    "toolkit", "wire", "vector", "render", "shell", "server", "client", "kit", "auto",
    "easy", "quick", "mini", "big", "small", "simple", "core", "base", "tree", "grid",
    "path", "link", "node", "graph", "graphs", "chain", "matrix", "diagram", "plot",
    "flag", "tag", "meta", "id", "key", "value",
}

# --- Second-pass refinements found by inspecting the first extraction ---
LIBRARIES |= {
    "filekit", "fileki", "filek", "composemultiplatformmediaplayer", "zxing",
    "mlkit", "ml kit", "jetpack", "parcelize", "kapt", "ksp", "cinterop", "cinter",
    "gradle", "keyclo", "keycloak", "toaster", "expo", "camerax", "media3", "kmmda",
    "androix", "serialport", "kswift", "concurrenthashmap", "minecraft", "poi",
    "kdroid", "ktorm", "ovcompose", "kmap", "klocation", "ksafe", "liquid", "story",
    "kotlin serialization", "kotlin-serialization",
    # ktor client artifact variants
    "ktor client", "ktor-client", "ktor-client-core", "ktor-client-cio",
    "ktor-client-darwin", "ktor-network", "ktor-serialization-kotlinx-json",
    "ktor log", "k'to'r", "kt", "kmp-", "sqld",
}
FRAGMENTS |= {
    "ca", "cal", "locali", "sq", "alar", "ana", "anal", "analy", "analyt", "analytic",
    "and", "backg", "brow", "cam", "circ", "cr", "cup", "da", "da ta", "datatime",
    "ff", "file pi", "file pick", "i18", "ico", "image pic", "key va", "lan", "life",
    "lot", "mapp", "mark", "md", "micro", "mediap", "mok", "multi", "navigation-",
    "not", "orienta", "oss", "p'er", "package s", "package sear", "pagina", "persis",
    "pref", "pullref", "pullto", "pullrefresh", "pulltorefresh", "qr sc", "qrc",
    "qu", "refle", "sand", "sharedpre", "snack", "soc", "toas", "valida", "we b",
    "work mana", "you", "zooma", "navigation 3", "navigation compose", "compose na",
    "real", "sorted", "even", "webviews", "down", "fit", "hot", "live", "open",
    "play", "pass", "page", "pick", "scan", "search", "service", "const", "cv",
    "rtc", "webrt", "cio", "backhandler", "statusbar", "videoplayer", "mediaplayer",
    "datepicker", "qrcode", "richtext", "bignum",
}

URL_JUNK = {"%2f", "%23", "%5c", "%27", "#", "+", "#n/a", "n/a"}


def normalize(raw: str) -> str:
    s = urllib.parse.unquote(raw.strip())
    s = s.replace("+", " ").strip().lower()
    s = re.sub(r"\s+", " ", s)
    return s


def is_noise(term: str) -> bool:
    if not term or term in URL_JUNK:
        return True
    if term == "#n/a" or term == "n/a":
        return True
    # non-latin (e.g. CJK) or pure punctuation
    if not re.search(r"[a-z0-9]", term):
        return True
    if re.search(r"[^\x00-\x7f]", term):
        return True
    # Maven coordinates: group.artifact / with ':' separators
    if ":" in term or ("." in term and " " not in term):
        return True
    # single character
    if len(term) <= 1:
        return True
    return False


def classify(term: str) -> str:
    if is_noise(term):
        return "noise"
    if term in LIBRARIES:
        return "library"
    if term in FRAGMENTS:
        return "fragment"
    return "concept"


def main(raw_path: str, out_path: str) -> None:
    with open(raw_path, encoding="utf-8") as f:
        raw_lines = [line for line in f.read().splitlines() if line.strip()]

    buckets = {"concept": Counter(), "library": Counter(), "fragment": Counter(), "noise": Counter()}
    for raw in raw_lines:
        term = normalize(raw)
        buckets[classify(term)][term] += 1

    concepts = sorted(buckets["concept"].items(), key=lambda kv: (-kv[1], kv[0]))

    with open(out_path, "w", encoding="utf-8") as f:
        f.write("# Concept search queries extracted from real klibs.io search logs.\n")
        f.write("# Library / brand names (ktor, koin, coil, ...) and noise fragments were removed.\n")
        f.write("# Format: <query><TAB><frequency in raw log>. Sorted by frequency, then alphabetically.\n")
        for term, freq in concepts:
            f.write(f"{term}\t{freq}\n")

    total = len(raw_lines)
    print(f"raw lines:      {total}")
    for name in ("concept", "library", "fragment", "noise"):
        print(f"{name:9}: {sum(buckets[name].values()):5} rows, {len(buckets[name]):4} distinct")
    print(f"\nwrote {len(concepts)} distinct concept queries to {out_path}")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    main(sys.argv[1], sys.argv[2])
