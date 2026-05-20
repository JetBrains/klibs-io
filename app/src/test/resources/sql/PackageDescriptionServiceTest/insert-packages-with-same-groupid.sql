
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (8003, 8003, 0, CURRENT_TIMESTAMP, 'test-user-3', 'author', 'Test User 3', 'Test user description', NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (8003, 8003, 8003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-3', 'Test repository 3', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (8004, 8004, 8003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-4', 'Test repository 4', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project VALUES (8003, 8003, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-3', NULL, 8003);
INSERT INTO public.project VALUES (8004, 8004, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-4', NULL, 8003);


INSERT INTO public.maven_artifact (id, group_id, artifact_id, version)
VALUES (8004, 'org.example', 'test-library', '1.0.0'),
       (8005, 'org.example', 'test-utils', '1.0.0');


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, scraper_type, generated_description, maven_artifact_id) VALUES (8004, 8003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'test-library', '1.0.0', 'Old description 1', 'https://example.com/test-library', NULL, 'gradle', '7.0', '1.6.0', NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN', false, 8004);

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, scraper_type, generated_description, maven_artifact_id) VALUES (8005, 8004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'test-utils', '1.0.0', 'Old description 2', 'https://example.com/test-utils', NULL, 'gradle', '7.0', '1.6.0', NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN', false, 8005);

INSERT INTO public.package_target (package_id, platform, target) VALUES (8004, 'JVM', '1.8');
INSERT INTO public.package_target (package_id, platform, target) VALUES (8005, 'JVM', '1.8');