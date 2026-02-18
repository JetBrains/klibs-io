-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (8002, 8002, 0, CURRENT_TIMESTAMP, 'test-user-2', 'author', 'Test User 2', 'Test user description', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (8002, 8002, 8002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-2', 'Test repository 2', NULL, 'mit', 'MIT License', 'main');

-- Insert test project
INSERT INTO public.project VALUES (8002, 8002, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-2', NULL, 8002);

-- Insert test packages with same groupId and artifactId but different versions
-- Older version
INSERT INTO public.package VALUES (8002, 8002, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP - INTERVAL '1 year', 'org.example', 'test-library', '1.0.0', 'Old description', 'https://example.com/test-library', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

-- Newer version (should be selected by the test)
INSERT INTO public.package VALUES (8003, 8002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'test-library', '2.0.0', 'Old description 2', 'https://example.com/test-library', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');