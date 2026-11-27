INSERT INTO public.scm_owner (id_native, id, followers, updated_at, login, type, name, description, homepage,
                              twitter_handle, email, location, company)
VALUES (6955922, 501, 0, current_timestamp, 'androidx', 'organization', 'AndroidX', null, null, null, null, null, null);

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key,
                             license_name, default_branch)
VALUES (96496250, 601, 501, false, false, false, true, '2017-07-07 00:00:00.000000', current_timestamp,
        '2026-01-01 00:00:00.000000', 0, 0, 'androidx', null, null, 'apache-2.0', 'Apache License 2.0',
        'androidx-main');
