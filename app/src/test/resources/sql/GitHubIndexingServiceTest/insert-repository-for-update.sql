-- Connected owner
INSERT INTO public.scm_owner (id_native, id, followers, updated_at, login, type, name, description, homepage,
                              twitter_handle, email, location, company)
VALUES (118642511,
        198,
        0,
        current_timestamp,
        'k-libs',
        'organization',
        'k-libs',
        null, null, null, null, null, null);

-- Repository for the test
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key,
                             license_name, default_branch)
VALUES (598863246,
        368,
        198,
        true,
        true,
        true,
        true,
        '2023-02-08 01:28:54.000000',
        current_timestamp - interval '24 hours',
        '2023-02-19 17:44:36.000000',
        0,
        0,
        'k-big-numbers',
        null,
        null,
        'mit',
        'MIT License',
        'main');

INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id)
VALUES (10001,
        368,
        current_timestamp,
        '1.0.0',
        'Initial description',
        'k-big-numbers',
        'initial minimized readme',
        198);
