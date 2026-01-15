-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (8003, 8003, 0, CURRENT_TIMESTAMP, 'test-user-3', 'author', 'Test User 3', 'Test user description', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repos
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (8003, 8003, 8003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-3', 'Test repository 3', NULL, 'mit', 'MIT License', 'main', 'Test readme content');

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (8004, 8004, 8003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-4', 'Test repository 4', NULL, 'mit', 'MIT License', 'main', 'Test readme content');

-- Insert test projects
INSERT INTO public.project VALUES (8003, 8003, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP);
INSERT INTO public.project VALUES (8004, 8004, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP);

-- Insert test packages with same groupId but different artifactIds
INSERT INTO public.package VALUES (8004, 8003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'test-library', '1.0.0', 'org.example:test-library', 'Old description 1', 'https://example.com/test-library', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

INSERT INTO public.package VALUES (8005, 8004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'test-utils', '1.0.0', 'org.example:test-utils', 'Old description 2', 'https://example.com/test-utils', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');