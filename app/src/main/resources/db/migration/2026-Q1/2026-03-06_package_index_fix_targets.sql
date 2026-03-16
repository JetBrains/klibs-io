DROP MATERIALIZED VIEW IF EXISTS package_index;

CREATE MATERIALIZED VIEW package_index AS
WITH LatestVersions AS (SELECT DISTINCT ON (p.group_id, p.artifact_id) p.group_id,
                                                                       p.artifact_id,
                                                                       p.version                                                    as latest_version,
                                                                       p.description                                                as latest_description,
                                                                       p.release_ts,
                                                                       p.licenses                                                   as latest_licenses,
                                                                       (SELECT jsonb_array_elements(p.licenses) ->> 'name' LIMIT 1) AS latest_license_name,
                                                                       p.project_id,
                                                                       p.id                                                         as latest_package_id
                        FROM package p
                        ORDER BY p.group_id, p.artifact_id, p.release_ts DESC)
SELECT p.group_id,
       p.artifact_id,
       p.project_id,
       p.latest_package_id,
       p.latest_version,
       p.latest_description,
       p.release_ts,
       scm_owner.type                                                     as owner_type,
       scm_owner.login                                                    as owner_login,
       p.latest_license_name,
       array_agg(DISTINCT pt.platform)                                    AS platforms,
       array_to_tsvector(array_agg(DISTINCT pt.platform))                 AS platforms_vector,
       tgt.targets,
       -- Form a tsvector from the targets for searching
       array_to_tsvector(array_remove(array_agg(DISTINCT COALESCE(pt.platform || '_' || pt.target, pt.platform)),
                                      NULL))                              AS targets_vector,
       (setweight(format('%s:1', p.group_id)::tsvector, 'A') ||
        setweight(format('%s:1', p.artifact_id)::tsvector, 'A') ||
        setweight(to_tsvector(replace(p.group_id, '.', ' ')), 'A') ||
        setweight(to_tsvector(replace(p.artifact_id, '.', ' ')), 'A') ||
        setweight(to_tsvector(coalesce(scm_owner.login, '')), 'A') ||
        setweight(to_tsvector(coalesce(p.latest_description, '')), 'B') ||
        setweight(to_tsvector(coalesce(array_to_string(array_agg(DISTINCT pt.platform), ' '), '')), 'C') ||
        setweight(to_tsvector(coalesce(p.latest_license_name, '')), 'C')) AS fts
FROM LatestVersions p
         JOIN project ON p.project_id = project.id
         JOIN scm_repo ON project.scm_repo_id = scm_repo.id
         JOIN scm_owner ON scm_repo.owner_id = scm_owner.id
         LEFT JOIN package_target pt ON p.latest_package_id = pt.package_id
         LEFT JOIN LATERAL (
    SELECT jsonb_object_agg(platform, targets) AS targets
    FROM (SELECT pt_sub.platform,
                 to_jsonb(array_remove(array_agg(DISTINCT pt_sub.target), NULL)) AS targets
          FROM package_target pt_sub
          WHERE pt_sub.package_id = p.latest_package_id
          GROUP BY pt_sub.platform) s
    ) tgt ON true
GROUP BY p.group_id,
         p.artifact_id,
         p.project_id,
         p.latest_package_id,
         p.latest_version,
         p.latest_description,
         p.release_ts,
         scm_owner.type,
         scm_owner.login,
         p.latest_licenses,
         p.latest_license_name,
         tgt.targets
WITH DATA;