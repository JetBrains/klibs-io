DROP MATERIALIZED VIEW IF EXISTS package_index;

CREATE MATERIALIZED VIEW package_index AS
WITH package_platforms AS (
    SELECT 
        package_id, 
        array_agg(DISTINCT platform) AS platforms,
        array_to_tsvector(array_agg(DISTINCT platform)) AS platforms_vector
    FROM
        package_target
    GROUP BY 
        package_id
)
SELECT
    p.id AS package_id,
    p.group_id,
    p.artifact_id,
    p.version,
    p.description,
    p.release_ts,
    scm_owner.type as owner_type,
    scm_owner.login as owner_login,
    (SELECT jsonb_array_elements(p.licenses)->>'name' LIMIT 1) AS license_name,
    pp.platforms,
    pp.platforms_vector,
    (setweight(format('%s:1', p.group_id)::tsvector, 'A') ||
     setweight(format('%s:1', p.artifact_id)::tsvector, 'A') ||
     setweight(to_tsvector(replace(p.group_id, '.', ' ')), 'A') ||
     setweight(to_tsvector(replace(p.artifact_id, '.', ' ')), 'A') ||
     setweight(to_tsvector(coalesce(scm_owner.login, '')), 'A') ||
     setweight(to_tsvector(coalesce(p.description, '')), 'B') ||
     setweight(to_tsvector(coalesce(array_to_string(pp.platforms, ' '), '')), 'C') ||
     setweight(to_tsvector(coalesce(license_name, '')), 'C')) AS fts
FROM 
    package p
    JOIN project ON p.project_id = project.id
    JOIN scm_repo ON project.scm_repo_id = scm_repo.id
    JOIN scm_owner ON scm_repo.owner_id = scm_owner.id
    LEFT JOIN package_platforms pp ON p.id = pp.package_id;
