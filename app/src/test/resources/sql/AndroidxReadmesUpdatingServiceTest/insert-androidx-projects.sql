-- The androidx owner with its single androidx/androidx repository, plus an unrelated owner and repository
INSERT INTO public.scm_owner (id_native, id, followers, updated_at, login, type, name, description, homepage,
                              twitter_handle, email, location, company)
VALUES (6955922, 501, 0, current_timestamp, 'androidx', 'organization', 'AndroidX', null, null, null, null, null, null),
       (118642511, 502, 0, current_timestamp, 'other', 'organization', 'other', null, null, null, null, null, null);

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key,
                             license_name, default_branch)
VALUES (96496250, 601, 501, false, false, false, true, '2017-07-07 00:00:00.000000', current_timestamp,
        '2026-01-01 00:00:00.000000', 0, 0, 'androidx', null, null, 'apache-2.0', 'Apache License 2.0',
        'androidx-main'),
       (598863246, 602, 502, true, true, true, true, '2023-02-08 01:28:54.000000', current_timestamp,
        '2023-02-19 17:44:36.000000', 0, 0, 'other-repo', null, null, 'mit', 'MIT License', 'main');

INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme,
                            owner_id)
VALUES
    -- The umbrella project, never synced
    (7001, 601, '2026-01-01 00:00:00.000000', '1.0.0', 'Umbrella description', 'androidx',
     'umbrella minimized readme', 501),
    -- A freshly indexed project: description must match ANDROIDX_DEFAULT_DESCRIPTION, no readme yet
    (7002, 601, '2026-01-01 00:00:00.000000', '3.2.0',
     'This is androidx library, that was not documented or supported well. It is probably just a KMP module, that could be used on your own responsibility.',
     'paging', null, 501),
    -- A project that already has a description and a readme
    (7003, 601, '2026-01-01 00:00:00.000000', '1.7.0', 'Compose description', 'compose',
     'old compose minimized readme', 501),
    -- A project of another owner, never synced
    (7004, 602, '2026-01-01 00:00:00.000000', '1.0.0', 'Other description', 'other-repo',
     'big numbers minimized readme', 502);
