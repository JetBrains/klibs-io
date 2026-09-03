WITH latest_pkg AS (
  SELECT DISTINCT ON (p.group_id, p.artifact_id)
         p.id AS package_id, p.group_id, p.artifact_id, p.project_id, p.description
  FROM package p
  ORDER BY p.group_id, p.artifact_id, p.release_ts DESC
),
proj_tgt AS (
  SELECT lp.project_id,
         array_remove(array_agg(DISTINCT pt.platform), NULL) AS platforms,
         array_remove(array_agg(DISTINCT COALESCE(pt.platform || '_' || pt.target, pt.platform)), NULL) AS targets
  FROM latest_pkg lp
    LEFT JOIN package_target pt ON pt.package_id = lp.package_id
  GROUP BY lp.project_id
),
gav AS (
  SELECT project_id,
         string_agg(DISTINCT group_id, ' ') AS group_ids,
         string_agg(DISTINCT artifact_id, ' ') AS artifact_ids,
         json_agg(
             json_build_object(
                 'group_id', group_id,
                 'artifact_id', artifact_id,
                 'latest_description', description
             )
             ORDER BY group_id, artifact_id
         ) AS packages
  FROM latest_pkg
  GROUP BY project_id
),
markers_info AS (
  SELECT project_id, array_agg(DISTINCT type) AS markers FROM project_marker GROUP BY project_id
),
tags_info AS (
  SELECT project_id, COALESCE(
           array_agg(DISTINCT value ORDER BY value DESC) FILTER (WHERE origin = 'USER'),
           array_agg(DISTINCT value ORDER BY value DESC) FILTER (WHERE origin = 'GITHUB'),
           array_agg(DISTINCT value ORDER BY value DESC) FILTER (WHERE origin = 'AI')
         ) AS tags
  FROM project_tags GROUP BY project_id
)
SELECT json_build_object(
  'project_id', project.id,
  'owner_type', owner.type,
  'owner_login', owner.login,
  'repo_name', repo.name,
  'name', project.name,
  'stars', repo.stars,
  'license_name', repo.license_name,
  'latest_version', project.latest_version,
  'latest_version_ts', project.latest_version_ts,
  'platforms', COALESCE(proj_tgt.platforms, ARRAY[]::text[]),
  'targets', COALESCE(proj_tgt.targets, ARRAY[]::text[]),
  'plain_description', COALESCE(project.description, repo.description),
  'project_description', project.description,
  'repo_description', repo.description,
  'tags', COALESCE(tags_info.tags, ARRAY[]::varchar[]),
  'markers', COALESCE(markers_info.markers, ARRAY[]::varchar[]),
  'group_ids', COALESCE(gav.group_ids, ''),
  'artifact_ids', COALESCE(gav.artifact_ids, ''),
  'dependent_count', project.dependent_count,
  'health_score', health.health_score,
  'has_readme', (project.minimized_readme IS NOT NULL),
  'packages', COALESCE(gav.packages, '[]'::json)
)::text
FROM project
  JOIN scm_owner owner ON project.owner_id = owner.id
  JOIN scm_repo repo ON project.scm_repo_id = repo.id
  JOIN proj_tgt ON proj_tgt.project_id = project.id
  LEFT JOIN gav ON gav.project_id = project.id
  LEFT JOIN markers_info ON markers_info.project_id = project.id
  LEFT JOIN tags_info ON tags_info.project_id = project.id
  LEFT JOIN scm_repo_health_components health ON health.scm_repo_id = repo.id
WHERE NOT EXISTS (SELECT 1 FROM project_hidden ph WHERE ph.project_id = project.id)
