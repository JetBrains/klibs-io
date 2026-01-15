-- Insert owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (7001, 7001, 0, CURRENT_TIMESTAMP, 'unknown-owner', 'organization', 'Unknown Owner', NULL, NULL, NULL, NULL, NULL, NULL);

-- Insert repository
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (7001, 7001, 7001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 'unknown-repo', 'Repo for unknown target test', NULL, 'apache-2.0', 'Apache License 2.0', 'main', 'Readme content');

-- Insert project
INSERT INTO public.project VALUES (7001, 7001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP);

-- Insert a package (single latest version)
-- Columns as used in other tests to match schema
INSERT INTO public.package 
VALUES (
  7001,              -- id
  7001,              -- project_id
  CURRENT_TIMESTAMP, -- created_ts
  CURRENT_TIMESTAMP, -- updated_at
  'org.example.unknown', -- group_id
  'unknown-target-lib',  -- artifact_id
  '1.0.0',          -- version
  'Unknown Target Lib', -- name
  'Library with unknown native target', -- description
  'https://example.com/unknown',        -- homepage
  'https://example.com/unknown',        -- scm_link (or similar extra url)
  'gradle',         -- build_tool
  '8.5',            -- build_tool_version
  '1.9.0',          -- kotlin_version
  null,             -- configuration
  '[]'::jsonb,      -- developers
  '[]'::jsonb,      -- licenses
  'SEARCH_MAVEN'   -- search_source
);

-- Insert package target with an unknown target value
INSERT INTO public.package_target (id, platform, target, package_id)
VALUES (7001, 'NATIVE', 'mystery_os_9000', 7001);

-- Insert package target with an known target value
INSERT INTO public.package_target (id, platform, target, package_id)
VALUES (7002, 'JVM', '18', 7001);
