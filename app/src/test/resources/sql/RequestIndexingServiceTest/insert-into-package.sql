INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (19001, 19001, 0, CURRENT_TIMESTAMP, 'owner-9001', 'author', 'Owner 9001', 'Owner desc', NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (19001, 19001, 19001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-9001', 'Repo 9001', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.project VALUES (19001, 19001, CURRENT_TIMESTAMP, '2.0.0', CURRENT_TIMESTAMP, 'repo-9001', 'readme', 19001);

INSERT INTO public.package VALUES (19001, 19001, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP, 'com.example', 'lib', '1.0.0', 'New', 'https://example.com/lib', 'https://example.com/lib', 'gradle', '7.0', '1.9.0', null,'[]'::jsonb,  '[]'::jsonb, 'SEARCH_MAVEN', true);
INSERT INTO public.package VALUES (19002, 19001, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP, 'com.example', 'libA', '1.0.0', 'Old A', 'https://example.com/libA', 'https://example.com/libA', 'gradle', '7.0', '1.9.0', null,'[]'::jsonb,  '[]'::jsonb, 'SEARCH_MAVEN', true);
INSERT INTO public.package VALUES (19003, 19001, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP, 'com.example', 'libA', '2.0.0', 'New A', 'https://example.com/libA', 'https://example.com/libA', 'gradle', '7.0', '1.9.0', null,'[]'::jsonb,  '[]'::jsonb, 'SEARCH_MAVEN', true);
INSERT INTO public.package VALUES (19004, 19001, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP, 'com.example', 'libB', '1.0.0', 'New B', 'https://example.com/libB', 'https://example.com/libB', 'gradle', '7.0', '1.9.0', null,'[]'::jsonb,  '[]'::jsonb, 'SEARCH_MAVEN', true);

INSERT INTO public.package_target (package_id, platform, target) VALUES (19002, 'NATIVE', 'linux_x64');
