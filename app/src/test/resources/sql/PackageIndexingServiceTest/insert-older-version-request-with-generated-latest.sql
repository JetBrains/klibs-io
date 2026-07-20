INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (9201, 9201, 0, CURRENT_TIMESTAMP, 'test-user-older', 'author', 'Test User Older', 'Test user description', NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9201, 9201, 9201, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-older', 'Test repository older', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project VALUES (9201, 9201, CURRENT_TIMESTAMP, '2.0.0', CURRENT_TIMESTAMP, 'test-repo-older', NULL, 9201);


INSERT INTO public.maven_artifact (id, group_id, artifact_id, version)
VALUES (9201, 'com.example', 'test-library-older', '2.0.0');


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, scraper_type, generated_description, maven_artifact_id) VALUES (9201, 9201, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'test-library-older', '2.0.0', 'This is a generated description for the latest version 2.0.0', 'https://example.com/test-library', NULL, 'gradle', '7.0', '1.6.0', NULL, '[]'::jsonb, '[]'::jsonb, 'CENTRAL_SONATYPE', true, 9201);


INSERT INTO package_index_request(id, group_id, artifact_id, version, released_ts, scraper_type, reindex, failed_attempts, status)
VALUES (9201, 'com.example', 'test-library-older', '1.0.0', CURRENT_TIMESTAMP - INTERVAL '1 month', 'CENTRAL_SONATYPE', false, 0, 'PENDING');
