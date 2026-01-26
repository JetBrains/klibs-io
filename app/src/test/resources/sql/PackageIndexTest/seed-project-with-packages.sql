-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (19001, 19001, 0, CURRENT_TIMESTAMP, 'owner-9001', 'author', 'Owner 9001', 'Owner desc', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (19001, 19001, 19001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-9001', 'Repo 9001', NULL, 'mit', 'MIT License', 'main', 'readme');

-- Insert different scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (18001, 18001, 19001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-8001', 'Repo 8001', NULL, 'mit', 'MIT License', 'main', 'readme');

-- Insert scm_repo for no packages
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (19100, 19100, 19001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-9100', 'Repo 9100', NULL, 'mit', 'MIT License', 'main', 'readme');

-- Insert test project
INSERT INTO public.project VALUES (19001, 19001, CURRENT_TIMESTAMP, '2.0.0', CURRENT_TIMESTAMP);

-- Insert different project
INSERT INTO public.project VALUES (18001, 18001, CURRENT_TIMESTAMP, '2.0.0', CURRENT_TIMESTAMP);

-- Insert test project without packages
INSERT INTO public.project VALUES (19100, 19100, CURRENT_TIMESTAMP, '0.0.0', CURRENT_TIMESTAMP);

-- Insert packages for artifact libA (older and newer)
-- Older version for libA
INSERT INTO public.package VALUES (19002, 19001, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP - INTERVAL '1 year', 'org.example', 'libA', '1.0.0', 'Old A', 'https://example.com/libA', 'gradle', '7.0', '1.9.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

-- Newer version for libA (latest)
INSERT INTO public.package VALUES (19003, 19001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'libA', '2.0.0', 'New A', 'https://example.com/libA', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

-- Package for artifact libB (single latest)
INSERT INTO public.package VALUES (19004, 19001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'libB', '3.1.4',  'Lib B', 'https://example.com/libB', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

-- Package for different project
INSERT INTO public.package VALUES (18002, 18001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.second', 'libC', '2.0.0',  'Lib C', 'https://second.com/libC', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

-- Targets for older libA (should NOT be counted for libA)
INSERT INTO public.package_target (package_id, platform, target) VALUES (19002, 'NATIVE', 'linux_x64');

-- Targets for latest libA (should be counted)
INSERT INTO public.package_target (package_id, platform, target) VALUES (19003, 'JVM', '1.8');
INSERT INTO public.package_target (package_id, platform, target) VALUES (19003, 'JS', NULL);

-- Targets for libB (should be counted)
INSERT INTO public.package_target (package_id, platform, target) VALUES (19004, 'JS', NULL);

-- Targets for second project
INSERT INTO public.package_target (package_id, platform, target) VALUES (18002, 'JS', NULL);

-- Refresh the materialized view to reflect the inserts
REFRESH MATERIALIZED VIEW package_index;