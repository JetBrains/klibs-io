-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (9001, 9001, 0, CURRENT_TIMESTAMP, 'test-user', 'author', 'Test User', 'Test user description', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9001, 9001, 9001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-1', 'Test repository 1', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9002, 9002, 9001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-2', 'Test repository 2', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9003, 9003, 9001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-3', 'Test repository 3', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9004, 9004, 9001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-4', 'Test repository 4', NULL, 'mit', 'MIT License', 'main');

-- Insert test projects
INSERT INTO public.project VALUES (9001, 9001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-1', NULL, 9001);

INSERT INTO public.project VALUES (9002, 9002, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-2', NULL, 9001);

INSERT INTO public.project VALUES (9003, 9003, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-3', NULL, 9001);

INSERT INTO public.project VALUES (9004, 9004, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-4', NULL, 9001);

-- Insert test packages with duplicate descriptions
-- First package with two versions - older version
INSERT INTO public.package VALUES (9001, 9001, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP - INTERVAL '1 year', 'com.example', 'http-client', '1.0.0', 'HTTP Client', 'Kotlin library for HTTP requests', 'https://example.com/http-client', 'https://example.com/http-client', 'gradle', '7.0', '1.6.0', null, '[]'::jsonb,'[]'::jsonb,  'SEARCH_MAVEN', false);

-- First package with two versions - newer version
INSERT INTO public.package VALUES (9005, 9001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'http-client', '2.0.0', 'HTTP Client', 'Kotlin library for HTTP requests', 'https://example.com/http-client', 'https://example.com/http-client','gradle', '7.0', '1.6.0', null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN', false);

-- Second package with two versions - older version
INSERT INTO public.package VALUES (9002, 9002, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP - INTERVAL '1 year', 'com.example', 'http-lib', '1.0.0', 'HTTP Library', 'Kotlin library for HTTP requests', 'https://example.com/http-client','https://example.com/http-lib', 'gradle', '7.0', '1.6.0', null, '[]'::jsonb, '[]'::jsonb,  'SEARCH_MAVEN', false);

-- Second package with two versions - newer version
INSERT INTO public.package VALUES (9006, 9002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'http-lib', '2.0.0', 'HTTP Library', 'Kotlin library for HTTP requests', 'https://example.com/http-lib', 'https://example.com/http-client','gradle', '7.0', '1.6.0', null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN', false);

-- Third package with a different description
INSERT INTO public.package VALUES (9003, 9003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'json-lib', '1.0.0', 'JSON Library', 'JSON serialization library', 'https://example.com/json-lib', 'https://example.com/http-client','gradle', '7.0', '1.6.0', null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN', false);

-- Fourth package with a unique description
INSERT INTO public.package VALUES (9004, 9004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'unique-lib', '1.0.0', 'Unique Library', 'A unique library with no duplicates', 'https://example.com/unique-lib','https://example.com/http-client', 'gradle', '7.0', '1.6.0', null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN', false);
