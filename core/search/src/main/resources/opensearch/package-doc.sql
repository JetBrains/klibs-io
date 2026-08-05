WITH latest AS (
  SELECT DISTINCT ON (p.group_id, p.artifact_id)
         p.group_id, p.artifact_id, p.project_id, p.id AS latest_package_id,
         p.version AS latest_version, p.description AS latest_description, p.release_ts,
         (SELECT jsonb_array_elements(p.licenses) ->> 'name' LIMIT 1) AS latest_license_name
  FROM package p
  ORDER BY p.group_id, p.artifact_id, p.release_ts DESC
),
tgt AS (
  SELECT pt.package_id,
         array_remove(array_agg(DISTINCT pt.platform), NULL) AS platforms,
         array_remove(array_agg(DISTINCT COALESCE(pt.platform || '_' || pt.target, pt.platform)), NULL) AS targets
  FROM package_target pt GROUP BY pt.package_id
)
SELECT json_build_object(
  'group_id', l.group_id,
  'artifact_id', l.artifact_id,
  'project_id', l.project_id,
  'latest_package_id', l.latest_package_id,
  'latest_version', l.latest_version,
  'latest_description', l.latest_description,
  'release_ts', l.release_ts,
  'owner_type', owner.type,
  'owner_login', owner.login,
  'latest_license_name', l.latest_license_name,
  'platforms', COALESCE(tgt.platforms, ARRAY[]::text[]),
  'targets', COALESCE(tgt.targets, ARRAY[]::text[])
)::text
FROM latest l
  JOIN project ON l.project_id = project.id
  JOIN scm_repo ON project.scm_repo_id = scm_repo.id
  JOIN scm_owner owner ON scm_repo.owner_id = owner.id
  LEFT JOIN tgt ON tgt.package_id = l.latest_package_id
