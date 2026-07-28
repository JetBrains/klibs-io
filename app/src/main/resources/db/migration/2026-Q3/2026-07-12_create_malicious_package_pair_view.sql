-- Primary signal (KTL-4617): SAME project + SAME artifactId + DIFFERENT groupId.
-- Secondary clues (KTL-4618) are carried in the same row
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
