INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (9101, 9101, 0, CURRENT_TIMESTAMP, 'test-user-reindex', 'author', 'Test User Reindex', 'Test user description', NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9101, 9101, 9101, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-reindex', 'Test repository reindex', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project VALUES (9101, 9101, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-reindex', NULL, 9101);


INSERT INTO public.maven_artifact (id, group_id, artifact_id, version)
VALUES (9101, 'com.example', 'test-library-reindex', '1.0.0');


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, scraper_type, generated_description, description_generated_at, maven_artifact_id) VALUES (9101, 9101, CURRENT_TIMESTAMP - INTERVAL '1 month', CURRENT_TIMESTAMP - INTERVAL '1 month', 'com.example', 'test-library-reindex', '1.0.0', 'This is a generated description for version 1.0.0', 'https://example.com/test-library', NULL, 'gradle', '7.0', '1.6.0', NULL, '[]'::jsonb, '[]'::jsonb, 'CENTRAL_SONATYPE', true, CURRENT_TIMESTAMP - INTERVAL '10 days', 9101);


INSERT INTO package_index_request(id, group_id, artifact_id, version, released_ts, scraper_type, reindex, failed_attempts, status)
VALUES (9101, 'com.example', 'test-library-reindex', '1.0.0', CURRENT_TIMESTAMP, 'CENTRAL_SONATYPE', true, 0, 'PENDING');
