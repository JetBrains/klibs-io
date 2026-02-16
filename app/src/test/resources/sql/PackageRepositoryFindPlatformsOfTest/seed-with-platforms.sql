-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (9001, 9001, 0, CURRENT_TIMESTAMP, 'owner-9001', 'author', 'Owner 9001', 'Owner desc', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (9001, 9001, 9001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-9001', 'Repo 9001', NULL, 'mit', 'MIT License', 'main', 'readme');

-- Insert test project
INSERT INTO public.project VALUES (9001, 9001, CURRENT_TIMESTAMP, '2.0.0', CURRENT_TIMESTAMP, 'repo-9001', NULL, 9001);

-- Insert packages for artifact libA (older and newer)
-- Older version for libA
INSERT INTO public.package VALUES (9002, 9001, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP - INTERVAL '1 year', 'org.example', 'libA', '1.0.0', 'org.example:libA', 'Old A', 'https://example.com/libA', 'gradle', '7.0', '1.9.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

-- Newer version for libA (latest)
INSERT INTO public.package VALUES (9003, 9001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'libA', '2.0.0', 'org.example:libA', 'New A', 'https://example.com/libA', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

-- Package for artifact libB (single latest)
INSERT INTO public.package VALUES (9004, 9001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'libB', '3.1.4', 'org.example:libB', 'Lib B', 'https://example.com/libB', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

-- Targets for older libA (should NOT be counted for libA)
INSERT INTO public.package_target (package_id, platform, target) VALUES (9002, 'JS', NULL);

-- Targets for latest libA (should be counted)
INSERT INTO public.package_target (package_id, platform, target) VALUES (9003, 'JVM', '1.8');
INSERT INTO public.package_target (package_id, platform, target) VALUES (9003, 'NATIVE', 'linux_x64');

-- Targets for libB (should be counted)
INSERT INTO public.package_target (package_id, platform, target) VALUES (9004, 'JS', NULL);
