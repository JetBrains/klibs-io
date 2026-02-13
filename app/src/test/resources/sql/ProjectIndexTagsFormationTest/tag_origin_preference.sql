-- Seed minimal data for testing project_index.tags origin priority

-- Project 10001: has user + github + AI tags -> expect user
-- Project 10002: has github + AI tags -> expect github
-- Project 10003: has only AI tags -> expect AI

-- scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES 
  (10001, 10001, 0, CURRENT_TIMESTAMP, 'owner-10001', 'author', 'Owner 10001', NULL, NULL, NULL, NULL, NULL, NULL),
  (10002, 10002, 0, CURRENT_TIMESTAMP, 'owner-10002', 'author', 'Owner 10002', NULL, NULL, NULL, NULL, NULL, NULL),
  (10003, 10003, 0, CURRENT_TIMESTAMP, 'owner-10003', 'author', 'Owner 10003', NULL, NULL, NULL, NULL, NULL, NULL);

-- scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES
  (10001, 10001, 10001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0, 'repo-10001', 'Repo 10001', NULL, 'mit', 'MIT License', 'main'),
  (10002, 10002, 10002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0, 'repo-10002', 'Repo 10002', NULL, 'mit', 'MIT License', 'main'),
  (10003, 10003, 10003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0, 'repo-10003', 'Repo 10003', NULL, 'mit', 'MIT License', 'main');

-- project
INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id) VALUES (10001, 10001, CURRENT_TIMESTAMP, '1.0.0', 'P10001', 'repo-10001', NULL, 10001),
  (10002, 10002, CURRENT_TIMESTAMP, '1.0.0', 'P10002', 'repo-10002', NULL, 10002),
  (10003, 10003, CURRENT_TIMESTAMP, '1.0.0', 'P10003', 'repo-10003', NULL, 10003);

-- package
-- minimal single package per project to ensure package_index entry exists
INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, name, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses)
VALUES
  (11001, 10001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-a', '1.0.0', 'lib-a', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
  (11002, 10002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-b', '1.0.0', 'lib-b', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
  (11003, 10003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-c', '1.0.0', 'lib-c', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]');

-- package_target with at least one platform per package so project_index can unnest platforms
INSERT INTO public.package_target (package_id, platform, target)
VALUES 
  (11001, 'jvm', NULL),
  (11002, 'jvm', NULL),
  (11003, 'jvm', NULL);

-- project_tags for different origins
-- For 10001, user should win, even if other origins exist
INSERT INTO public.project_tags (project_id, origin, value) VALUES
  (10001, 'AI', 'ai-tag-1'),
  (10001, 'GITHUB', 'gh-tag-1'),
  (10001, 'USER', 'user-tag-1'),
  (10001, 'USER', 'user-tag-2');

-- For 10002, github should win (no user), despite AI existing
INSERT INTO public.project_tags (project_id, origin, value) VALUES
  (10002, 'AI', 'ai-tag-2'),
  (10002, 'GITHUB', 'gh-tag-2'),
  (10002, 'GITHUB', 'gh-tag-3');

-- For 10003, only AI present, so AI should be used
INSERT INTO public.project_tags (project_id, origin, value) VALUES
  (10003, 'AI', 'ai-tag-3'),
  (10003, 'AI', 'ai-tag-4');
