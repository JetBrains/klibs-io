DROP MATERIALIZED VIEW IF EXISTS project_index;

CREATE MATERIALIZED VIEW project_index AS
WITH package_info AS (
    SELECT project.id,
           array_agg(DISTINCT pckg_target.platform) AS platforms,
           array_to_tsvector(array_agg(DISTINCT pckg_target.platform)) AS platforms_vector,
           string_agg(format('%s:1', pckg.group_id), ' ')::tsvector AS group_ids_vector,
           string_agg(format('%s:2', pckg.artifact_id), ' ')::tsvector AS artifact_ids_vector
    FROM project
             JOIN scm_repo scm_repo ON project.scm_repo_id = scm_repo.id
             JOIN package pckg ON project.id = pckg.project_id AND project.latest_version = pckg.version
             JOIN package_target pckg_target ON pckg.id = pckg_target.package_id
    GROUP BY project.id
), markers_info AS (
    select project_marker.project_id,
           array_agg(DISTINCT project_marker.type) AS markers
    from project_marker
    GROUP BY project_marker.project_id
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
       project.tags AS tags,
       markers_info.markers as markers,
       (setweight(to_tsvector(owner.login), 'A') ||
        setweight(to_tsvector(repo.name), 'A') ||
        setweight(format('%s:1', repo.name)::tsvector, 'A') ||
        setweight(format('%s:1', owner.login)::tsvector, 'A') ||
        setweight(package_info.group_ids_vector, 'B') ||
        setweight(package_info.artifact_ids_vector, 'B') ||
        setweight(to_tsvector(coalesce(owner.name, '')), 'D') ||
        setweight(to_tsvector(coalesce(owner.description, '')), 'D') ||
        setweight(to_tsvector(coalesce(repo.minimized_readme, '')), 'D') ||
        setweight(to_tsvector(coalesce(project.description, '')), 'C') ||
        setweight(to_tsvector(coalesce(repo.description, '')), 'C') ||
        setweight(to_tsvector(coalesce(array_to_string(project.tags, ' '), '')), 'B')) AS fts
FROM project
         JOIN package_info ON project.id = package_info.id
         JOIN scm_repo repo ON project.scm_repo_id = repo.id
         JOIN scm_owner owner ON repo.owner_id = owner.id
         LEFT JOIN markers_info  on markers_info.project_id = project.id