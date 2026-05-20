
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (8001, 8001, 0, CURRENT_TIMESTAMP, 'test-user', 'author', 'Test User', 'Test user description', NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (8001, 8001, 8001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-1', 'Test repository 1', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (8002, 8002, 8001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-2', 'Test repository 2', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project VALUES (8001, 8001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-1', NULL, 8001);
INSERT INTO public.project VALUES (8002, 8002, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-2', NULL, 8001);




INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1014548742, 'org.example', 'http-client', '1.0.0'),
    (1029769046, 'org.example', 'http-lib', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (8001, 8001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'http-client', '1.0.0', 'Kotlin library for HTTP requests', 'https://example.com/http-client', 'gradle', '7.0', '1.6.0', '2.1.20', null, '[{"url": "mailto:rob@continuousexcellence.io", "name": "Rob Murdock"}]'::jsonb, 'SEARCH_MAVEN', 1014548742);


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (8002, 8002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'http-lib', '1.0.0', 'Kotlin library for HTTP requests', 'https://example.com/http-lib', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, 'SEARCH_MAVEN', 1029769046);

INSERT INTO public.package_target VALUES (8001, 'NATIVE', 'macos_arm64', 3029111);
INSERT INTO public.package_target VALUES (8002, 'NATIVE', 'macos_x64', 30301111);