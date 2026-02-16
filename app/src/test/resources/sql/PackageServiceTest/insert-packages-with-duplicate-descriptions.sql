-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (8001, 8001, 0, CURRENT_TIMESTAMP, 'test-user', 'author', 'Test User', 'Test user description', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (8001, 8001, 8001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-1', 'Test repository 1', NULL, 'mit', 'MIT License', 'main', 'Test readme content for repo 1');

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (8002, 8002, 8001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 10, 'test-repo-2', 'Test repository 2', NULL, 'mit', 'MIT License', 'main', 'Test readme content for repo 2');

-- Insert test projects
INSERT INTO public.project VALUES (8001, 8001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-1', NULL, 8001);
INSERT INTO public.project VALUES (8002, 8002, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'test-repo-2', NULL, 8001);

-- Insert test packages with duplicate descriptions
-- First package
INSERT INTO public.package VALUES (8001, 8001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'http-client', '1.0.0', 'HTTP Client', 'Kotlin library for HTTP requests', 'https://example.com/http-client', 'gradle', '7.0', '1.6.0', '2.1.20', null, '[{"url": "mailto:rob@continuousexcellence.io", "name": "Rob Murdock"}]'::jsonb, '[{"url": "https://github.com/robertfmurdock/ze-great-tools", "name": "MIT License"}]'::jsonb, 'SEARCH_MAVEN');

-- Second package with the same description
INSERT INTO public.package VALUES (8002, 8002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'http-lib', '1.0.0', 'HTTP Library', 'Kotlin library for HTTP requests', 'https://example.com/http-lib', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, '[{"url": "https://github.com/robertfmurdock/ze-great-tools", "name": "MIT License"}]'::jsonb, 'SEARCH_MAVEN');

INSERT INTO public.package_target VALUES (8001, 'NATIVE', 'macos_arm64', 3029111);
INSERT INTO public.package_target VALUES (8002, 'NATIVE', 'macos_x64', 30301111);