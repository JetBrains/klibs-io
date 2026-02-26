INSERT INTO public.scm_owner (id_native, id, followers, updated_at, login, type, name, description, homepage,
                              twitter_handle, email, location, company)
VALUES (118642511, 198, 0, current_timestamp, 'k-libs', 'organization', 'k-libs', null, null, null, null,
        null, null);

-- Repository for the test
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key,
                             license_name, default_branch)
VALUES (598863246, 368, 198, true, true, true, true, '2023-02-08 01:28:54.000000',
        current_timestamp - interval '24 hours',
        '2023-02-19 17:44:36.000000', 0, 0, 'k-big-numbers', null, null, 'mit', 'MIT License', 'main');

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key,
                             license_name, default_branch)
VALUES (598863247, 369, 198, true, true, true, true, '2023-02-08 01:28:54.000000',
        current_timestamp - interval '24 hours',
        '2023-02-19 17:44:36.000000', 0, 0, 'k-big-numbers', null, null, 'mit', 'MIT License', 'main');

-- Insert test projects
INSERT INTO public.project VALUES (10002, 368, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'k-big-numbers', NULL, 198);
INSERT INTO public.project VALUES (10003, 369, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'k-big-numbers', NULL, 198);

-- Insert test packages with same groupId but different artifactIds
INSERT INTO public.package VALUES (10002, 10002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'test-library', '1.0.0', 'Old description 1', 'https://example.com/test-library', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');
INSERT INTO public.package VALUES (10003, 10003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'test-utils', '1.0.0', 'Old description 2', 'https://example.com/test-utils', 'gradle', '7.0', '1.6.0', '[]'::jsonb, null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN');

INSERT INTO public.package_target (package_id, platform, target) VALUES (10002, 'JVM', '1.8');
INSERT INTO public.package_target (package_id, platform, target) VALUES (10003, 'JVM', '1.8');