-- This is a READ-ONLY analysis script.
--
-- Produces three result sets:
--   1. Summary   — one row per suspicious (project, artifactId) with all groupIds
--   2. Detail    — one row per groupId branch, with versions/timestamps/metadata
--   3. Watchlist — flagged groupIds cross-checked against the banned_packages table

\pset pager off
\timing off

\set flagged_cte 'flagged AS (SELECT project_id, artifact_id FROM package WHERE project_id IS NOT NULL GROUP BY project_id, artifact_id HAVING count(DISTINCT group_id) > 1)'


\echo '==========================================================================='
\echo ' 1) SUMMARY — suspicious (project, artifactId) pairs, most groupIds first'
\echo '==========================================================================='

SELECT p.project_id,
       proj.name                                            AS project_name,
       owner.login                                          AS owner_login,
       owner.type                                           AS owner_type,
       repo.name                                            AS repo_name,
       p.artifact_id,
       count(DISTINCT p.group_id)                           AS distinct_group_ids,
       array_agg(DISTINCT p.group_id ORDER BY p.group_id)   AS group_ids,
       count(*)                                             AS package_rows
FROM package p
         JOIN project proj    ON proj.id = p.project_id
         JOIN scm_owner owner ON owner.id = proj.owner_id
         LEFT JOIN scm_repo repo ON repo.id = proj.scm_repo_id
WHERE p.project_id IS NOT NULL
GROUP BY p.project_id, proj.name, owner.login, owner.type, repo.name, p.artifact_id
HAVING count(DISTINCT p.group_id) > 1
ORDER BY distinct_group_ids DESC, owner.login, p.artifact_id;


\echo ''
\echo '==========================================================================='
\echo ' 2) DETAIL — per-groupId breakdown for each suspicious pair'
\echo '     (compare release windows / version counts to spot the impostor branch)'
\echo '==========================================================================='

WITH flagged AS (SELECT project_id, artifact_id
                 FROM package
                 WHERE project_id IS NOT NULL
                 GROUP BY project_id, artifact_id
                 HAVING count(DISTINCT group_id) > 1),
     latest AS (SELECT DISTINCT ON (project_id, artifact_id, group_id)
                    project_id, artifact_id, group_id, version AS latest_version, release_ts AS latest_release_ts
                FROM package
                ORDER BY project_id, artifact_id, group_id, release_ts DESC)
SELECT p.project_id,
       proj.name                                    AS project_name,
       owner.login                                  AS owner_login,
       p.artifact_id,
       p.group_id,
       count(*)                                     AS versions,
       l.latest_version,
       min(p.release_ts)                            AS first_release_ts,
       l.latest_release_ts,
       max(p.scm_url)                               AS scm_url,
       bool_or(p.generated_description)             AS has_generated_description
FROM package p
         JOIN flagged f      ON f.project_id = p.project_id AND f.artifact_id = p.artifact_id
         JOIN project proj   ON proj.id = p.project_id
         JOIN scm_owner owner ON owner.id = proj.owner_id
         JOIN latest l       ON l.project_id = p.project_id
                                    AND l.artifact_id = p.artifact_id
                                    AND l.group_id = p.group_id
GROUP BY p.project_id, proj.name, owner.login, p.artifact_id, p.group_id,
         l.latest_version, l.latest_release_ts
ORDER BY p.project_id, p.artifact_id, first_release_ts;


\echo ''
\echo '==========================================================================='
\echo ' 3) WATCHLIST — flagged groupIds already present in banned_packages'
\echo '==========================================================================='

WITH flagged AS (SELECT project_id, artifact_id
                 FROM package
                 WHERE project_id IS NOT NULL
                 GROUP BY project_id, artifact_id
                 HAVING count(DISTINCT group_id) > 1)
SELECT DISTINCT p.project_id,
                p.artifact_id,
                p.group_id,
                (bp.id IS NOT NULL) AS already_banned,
                bp.reason           AS ban_reason
FROM package p
         JOIN flagged f ON f.project_id = p.project_id AND f.artifact_id = p.artifact_id
         LEFT JOIN banned_packages bp
                   ON bp.group_id = p.group_id
                       AND (bp.artifact_id IS NULL OR bp.artifact_id = p.artifact_id)
ORDER BY already_banned DESC, p.project_id, p.artifact_id, p.group_id;
