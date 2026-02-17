-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (9001, 9001, 0, CURRENT_TIMESTAMP, 'test-user-3', 'author', 'Test User 3', 'Test user description', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9001, 9001, 9001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-3', 'Test repository 3', NULL, 'mit', 'MIT License', 'main');

-- Insert test project
INSERT INTO public.project VALUES (9001, 9001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-3', NULL, 9001);

-- Insert test package
INSERT INTO public.package VALUES (9001, 9001, CURRENT_TIMESTAMP - INTERVAL '1 month', CURRENT_TIMESTAMP - INTERVAL '1 month', 'com.example', 'test-library-gen', '1.0.0', 'test-library-gen', 'This is a description for version 1.0.0', 'https://example.com/test-library', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, '[]'::jsonb, 'CENTRAL_SONATYPE');

-- Insert indexing request for a new version of the same package
INSERT INTO package_index_request(id, group_id, artifact_id, version, released_ts, scraper_type, reindex, failed_attempts)
VALUES (9001, 'com.example', 'test-library-gen', '2.0.0', CURRENT_TIMESTAMP, 'CENTRAL_SONATYPE', false, 0);
