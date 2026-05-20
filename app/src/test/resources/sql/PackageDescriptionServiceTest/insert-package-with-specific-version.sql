
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (8001, 8001, 0, CURRENT_TIMESTAMP, 'test-user', 'author', 'Test User', 'Test user description', NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (8001, 8001, 8001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo', 'Test repository', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project VALUES (8001, 8001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo', NULL, 8001);


INSERT INTO public.maven_artifact (id, group_id, artifact_id, version)
VALUES (8001, 'org.example', 'test-library', '1.0.0');


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, scraper_type, generated_description, maven_artifact_id) VALUES (8001, 8001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'test-library', '1.0.0', 'Old description', 'https://example.com/test-library', NULL, 'gradle', '7.0', '1.6.0', NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN', false, 8001);