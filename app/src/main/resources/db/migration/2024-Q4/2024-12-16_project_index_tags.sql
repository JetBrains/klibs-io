DROP MATERIALIZED VIEW IF EXISTS project_index;

CREATE MATERIALIZED VIEW project_index AS
WITH package_info AS (
    SELECT project.id,
           array_agg(DISTINCT pckg_target.platform) AS platforms,
           array_to_tsvector(array_agg(DISTINCT pckg_target.platform)) AS platforms_vector,
           array_to_tsvector(array_agg(DISTINCT pckg.group_id)) AS group_ids_vector,
           array_to_tsvector(array_agg(DISTINCT pckg.artifact_id)) AS artifact_ids_vector
    FROM project
             JOIN scm_repo scm_repo ON project.scm_repo_id = scm_repo.id
             JOIN package pckg ON project.id = pckg.project_id AND project.latest_version = pckg.version
             JOIN package_target pckg_target ON pckg.id = pckg_target.package_id
    GROUP BY project.id
)
SELECT project.id AS project_id,
       owner.type AS owner_type,
       owner.login AS owner_login,
       repo.name,
       repo.stars,
       repo.license_name,
       project.latest_version,
       project.latest_version_ts,
       package_info.platforms,
       package_info.platforms_vector,
       coalesce(project.description, repo.description) AS plain_description,
       pt.tags_processed AS tags,
       (setweight(to_tsvector(owner.login), 'A') ||
        setweight(to_tsvector(repo.name), 'A') ||
        package_info.group_ids_vector ||
        package_info.artifact_ids_vector ||
        setweight(to_tsvector(coalesce(owner.name, '')), 'D') ||
        setweight(to_tsvector(coalesce(owner.description, '')), 'D') ||
        setweight(to_tsvector(coalesce(project.description, '')), 'C') ||
        setweight(to_tsvector(coalesce(repo.description, '')), 'C') ||
        setweight(to_tsvector(coalesce(array_to_string(pt.tags_processed, ' '), '')), 'B')) AS fts
FROM project
         JOIN package_info ON project.id = package_info.id
         JOIN scm_repo repo ON project.scm_repo_id = repo.id
         JOIN scm_owner owner ON repo.owner_id = owner.id
         LEFT JOIN project_tags pt ON repo.name = pt.project_name;