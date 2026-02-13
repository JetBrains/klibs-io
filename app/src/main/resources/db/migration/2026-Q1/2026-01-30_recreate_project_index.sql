DROP MATERIALIZED VIEW IF EXISTS project_index;

CREATE MATERIALIZED VIEW project_index AS
WITH package_info AS (
    SELECT project.id,
           array_agg(DISTINCT platform) AS platforms,
           array_to_tsvector(array_agg(DISTINCT platform)) AS platforms_vector,
           string_agg(format('%s:1', pckg.group_id), ' ')::tsvector AS group_ids_vector,
           string_agg(format('%s:2', pckg.artifact_id), ' ')::tsvector AS artifact_ids_vector,
           array_to_tsvector(array_remove(array_agg(DISTINCT target), NULL)) AS targets_vector
    FROM project
        JOIN package_index pckg ON project.id = pckg.project_id
        CROSS JOIN LATERAL unnest(pckg.platforms) as platform
        LEFT JOIN LATERAL unnest(pckg.targets) AS target ON true
    GROUP BY project.id
), markers_info AS (
    SELECT project_marker.project_id,
           array_agg(DISTINCT project_marker.type) AS markers
    FROM project_marker
    GROUP BY project_marker.project_id
), tags_info AS (
    SELECT
        project_id,
        COALESCE(
            array_agg(DISTINCT value ORDER BY value DESC) FILTER (WHERE origin = 'USER'),
            array_agg(DISTINCT value ORDER BY value DESC) FILTER (WHERE origin = 'GITHUB'),
            array_agg(DISTINCT value ORDER BY value DESC) FILTER (WHERE origin = 'AI')
        ) AS tags
    FROM project_tags
    GROUP BY project_id
)
SELECT project.id AS project_id,
       owner.type AS owner_type,
       owner.login AS owner_login,
       repo.name AS repo_name,
       project.name,
       repo.stars,
       repo.license_name,
       project.latest_version,
       project.latest_version_ts,
       package_info.platforms,
       package_info.platforms_vector,
       package_info.targets_vector,
       coalesce(project.description, repo.description) AS plain_description,
       tags_info.tags AS tags,
       markers_info.markers AS markers,
       (setweight(to_tsvector(owner.login), 'A') ||
        setweight(to_tsvector(project.name), 'A') ||
        setweight(format('%s:1', project.name)::tsvector, 'A') ||
        setweight(format('%s:1', owner.login)::tsvector, 'A') ||
        setweight(package_info.group_ids_vector, 'B') ||
        setweight(package_info.artifact_ids_vector, 'B') ||
        setweight(to_tsvector(coalesce(owner.name, '')), 'D') ||
        setweight(to_tsvector(coalesce(owner.description, '')), 'D') ||
        setweight(to_tsvector(coalesce(project.minimized_readme, '')), 'D') ||
        setweight(to_tsvector(coalesce(project.description, '')), 'C') ||
        setweight(to_tsvector(coalesce(repo.description, '')), 'C') ||
        setweight(to_tsvector(coalesce(array_to_string(tags_info.tags, ' '), '')), 'B')) AS fts
FROM project
         JOIN package_info ON project.id = package_info.id
         JOIN scm_owner owner ON project.owner_id = owner.id
         JOIN scm_repo repo ON project.scm_repo_id = repo.id
         LEFT JOIN markers_info on markers_info.project_id = project.id
         LEFT JOIN tags_info on tags_info.project_id = project.id;
