INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (9301, 9301, 0, CURRENT_TIMESTAMP, 'test-user-ttl', 'author', 'Test User TTL', 'Test user description', NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9301, 9301, 9301, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-ttl', 'Test repository ttl', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project VALUES (9301, 9301, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-ttl', NULL, 9301);


INSERT INTO public.maven_artifact (id, group_id, artifact_id, version)
VALUES (9301, 'com.example', 'test-library-ttl', '1.0.0');


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, scraper_type, generated_description, description_generated_at, maven_artifact_id) VALUES (9301, 9301, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day', 'com.example', 'test-library-ttl', '1.0.0', 'Recent AI description', 'https://example.com/test-library', NULL, 'gradle', '7.0', '1.6.0', NULL, '[]'::jsonb, '[]'::jsonb, 'CENTRAL_SONATYPE', true, CURRENT_TIMESTAMP - INTERVAL '1 day', 9301);


INSERT INTO package_index_request(id, group_id, artifact_id, version, released_ts, scraper_type, reindex, failed_attempts, status)
VALUES (9301, 'com.example', 'test-library-ttl', '2.0.0', CURRENT_TIMESTAMP, 'CENTRAL_SONATYPE', false, 0, 'PENDING');
