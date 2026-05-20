






INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES 
  (10001, 10001, 0, CURRENT_TIMESTAMP, 'owner-10001', 'author', 'Owner 10001', NULL, NULL, NULL, NULL, NULL, NULL),
  (10002, 10002, 0, CURRENT_TIMESTAMP, 'owner-10002', 'author', 'Owner 10002', NULL, NULL, NULL, NULL, NULL, NULL),
  (10003, 10003, 0, CURRENT_TIMESTAMP, 'owner-10003', 'author', 'Owner 10003', NULL, NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES
  (10001, 10001, 10001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0, 'repo-10001', 'Repo 10001', NULL, 'mit', 'MIT License', 'main'),
  (10002, 10002, 10002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0, 'repo-10002', 'Repo 10002', NULL, 'mit', 'MIT License', 'main'),
  (10003, 10003, 10003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 1, 0, 'repo-10003', 'Repo 10003', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id) VALUES (10001, 10001, CURRENT_TIMESTAMP, '1.0.0', 'P10001', 'repo-10001', NULL, 10001),
  (10002, 10002, CURRENT_TIMESTAMP, '1.0.0', 'P10002', 'repo-10002', NULL, 10002),
  (10003, 10003, CURRENT_TIMESTAMP, '1.0.0', 'P10003', 'repo-10003', NULL, 10003);




INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1075077997, 'com.example', 'lib-a', '1.0.0'),
    (1037489706, 'com.example', 'lib-b', '1.0.0'),
    (1080431850, 'com.example', 'lib-c', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, maven_artifact_id) VALUES (11001, 10001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-a', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1075077997),
       (11002, 10002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-b', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1037489706),
       (11003, 10003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-c', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1080431850);


INSERT INTO public.package_target (package_id, platform, target)
VALUES 
  (11001, 'jvm', NULL),
  (11002, 'jvm', NULL),
  (11003, 'jvm', NULL);



INSERT INTO public.project_tags (project_id, origin, value) VALUES
  (10001, 'AI', 'ai-tag-1'),
  (10001, 'GITHUB', 'gh-tag-1'),
  (10001, 'USER', 'user-tag-1'),
  (10001, 'USER', 'user-tag-2');


INSERT INTO public.project_tags (project_id, origin, value) VALUES
  (10002, 'AI', 'ai-tag-2'),
  (10002, 'GITHUB', 'gh-tag-2'),
  (10002, 'GITHUB', 'gh-tag-3');


INSERT INTO public.project_tags (project_id, origin, value) VALUES
  (10003, 'AI', 'ai-tag-3'),
  (10003, 'AI', 'ai-tag-4');
