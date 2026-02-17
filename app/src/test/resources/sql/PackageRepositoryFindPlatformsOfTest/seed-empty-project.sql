-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (9100, 9100, 0, CURRENT_TIMESTAMP, 'owner-9100', 'author', 'Owner 9100', 'Owner desc', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9100, 9100, 9100, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-9100', 'Repo 9100', NULL, 'mit', 'MIT License', 'main');

-- Insert test project without packages
INSERT INTO public.project VALUES (9100, 9100, CURRENT_TIMESTAMP, '0.0.0', CURRENT_TIMESTAMP, 'repo-9100', NULL, 9100);
