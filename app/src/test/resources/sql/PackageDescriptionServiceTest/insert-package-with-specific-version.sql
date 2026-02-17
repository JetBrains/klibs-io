-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (8001, 8001, 0, CURRENT_TIMESTAMP, 'test-user', 'author', 'Test User', 'Test user description', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (8001, 8001, 8001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo', 'Test repository', NULL, 'mit', 'MIT License', 'main');

-- Insert test project
INSERT INTO public.project VALUES (8001, 8001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo', NULL, 8001);

-- Insert test package with specific version
INSERT INTO public.package VALUES (8001, 8001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'test-library', '1.0.0', 'org.example:test-library', 'Old description', 'https://example.com/test-library', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');