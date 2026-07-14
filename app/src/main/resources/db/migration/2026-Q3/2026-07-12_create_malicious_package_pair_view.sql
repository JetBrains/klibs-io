-- KTL-4617 — potential malicious package pairs.
-- =============================================================================
-- Primary signal (KTL-4617): SAME project + SAME artifactId + DIFFERENT groupId.
-- Within one indexed project a library name (artifactId) should map to a single
-- publisher coordinate (groupId). When the same name appears under two or more
-- groupIds, one branch may be a typo-/name-squat or impersonation (e.g. a hostile
-- actor republishing a popular library under a look-alike groupId).
--
-- Secondary clues (KTL-4618) are carried in the same row so a reviewer can rank
-- branches without a second query:
--   * release_rank        — 1 = earliest first-publish = presumed ORIGINAL; a
--                           higher rank branch was published LATER (the ticket's
--                           main "malicious is usually released later" criterion).
--   * days_after_original — days between this branch's first publish and the
--                           earliest branch in the pair.
--   * versions            — squats tend to have very few releases.
--   * scm_url             — a mismatched/missing repo across branches is a red flag.
--   * has_generated_description — whether our AI wrote the description (weak clue).
--
-- Plain (non-materialized) VIEW: always live, no refresh job, no index. Release
-- dates come straight from `package` (min/max release_ts per branch), so the
-- first-publish date used for ranking is authoritative rather than the
-- latest-version date exposed by package_index.
--
-- One row per BRANCH (project_id, artifact_id, group_id) of a conflicting pair.
CREATE OR REPLACE VIEW malicious_package_pair AS
WITH conflicting AS (SELECT project_id, artifact_id
                     FROM package
                     WHERE project_id IS NOT NULL
                     GROUP BY project_id, artifact_id
                     HAVING count(DISTINCT group_id) > 1),
     branch AS (SELECT p.project_id,
                       p.artifact_id,
                       p.group_id,
                       count(*)                                             AS versions,
                       (array_agg(p.version ORDER BY p.release_ts DESC))[1] AS latest_version,
                       min(p.release_ts)                                    AS first_release_ts,
                       max(p.release_ts)                                    AS latest_release_ts,
                       (array_agg(p.scm_url ORDER BY p.release_ts DESC)
                            FILTER (WHERE p.scm_url IS NOT NULL))[1]         AS scm_url,
                       bool_or(p.generated_description)                     AS has_generated_description
                FROM package p
                         JOIN conflicting c
                              ON c.project_id = p.project_id AND c.artifact_id = p.artifact_id
                GROUP BY p.project_id, p.artifact_id, p.group_id)
SELECT b.project_id,
       proj.name                                             AS project_name,
       owner.login                                           AS owner_login,
       owner.type                                            AS owner_type,
       repo.name                                             AS repo_name,
       b.artifact_id,
       b.group_id,
       b.versions,
       b.latest_version,
       b.first_release_ts,
       b.latest_release_ts,
       b.scm_url,
       b.has_generated_description,
       count(*) OVER w                                        AS branch_count,
       array_agg(b.group_id) OVER w                           AS group_ids,
       rank() OVER (PARTITION BY b.project_id, b.artifact_id ORDER BY b.first_release_ts)
                                                              AS release_rank,
       (EXTRACT(EPOCH FROM (b.first_release_ts - min(b.first_release_ts) OVER w)) / 86400)::int
                                                              AS days_after_original
FROM branch b
         JOIN project proj    ON proj.id = b.project_id
         JOIN scm_owner owner ON owner.id = proj.owner_id
         LEFT JOIN scm_repo repo ON repo.id = proj.scm_repo_id
WINDOW w AS (PARTITION BY b.project_id, b.artifact_id);
