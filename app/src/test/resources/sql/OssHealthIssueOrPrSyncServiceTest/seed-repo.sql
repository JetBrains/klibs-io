-- One owner + one repo so the OSS health issue/PR sync flow has a real scm_repo row to attach
-- rows to. IDs are kept stable across tests so the test code can refer to them by constant.
INSERT INTO public.scm_owner (
    id, id_native, followers, updated_at,
    login, type, name, description,
    homepage, twitter_handle, email, location, company
) VALUES (
    700001, 700000001, 0, current_timestamp,
    'oss-health-test', 'organization', 'OSS Health Test', NULL,
    NULL, NULL, NULL, NULL, NULL
);

INSERT INTO public.scm_repo (
    id_native, id, owner_id,
    has_gh_pages, has_issues, has_wiki, has_readme,
    created_ts, updated_at, last_activity_ts,
    stars, open_issues,
    name, description, homepage,
    license_key, license_name, default_branch
) VALUES (
    700000002, 700002, 700001,
    false, true, false, false,
    current_timestamp, current_timestamp, current_timestamp,
    0, 0,
    'oss-health-test-repo', NULL, NULL,
    NULL, NULL, 'main'
);
