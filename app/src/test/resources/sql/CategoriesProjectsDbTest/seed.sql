-- Seed data for testing GET /categories/projects (CategoriesProjectsDbTest)

-- scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES
    (50001, 50001, 0, CURRENT_TIMESTAMP, 'cat-owner-a', 'author', 'Cat Owner A', NULL, NULL, NULL, NULL, NULL, NULL),
    (50002, 50002, 0, CURRENT_TIMESTAMP, 'cat-owner-b', 'author', 'Cat Owner B', NULL, NULL, NULL, NULL, NULL, NULL),
    (50003, 50003, 0, CURRENT_TIMESTAMP, 'cat-owner-c', 'author', 'Cat Owner C', NULL, NULL, NULL, NULL, NULL, NULL),
    (50004, 50004, 0, CURRENT_TIMESTAMP, 'cat-owner-d', 'author', 'Cat Owner D', NULL, NULL, NULL, NULL, NULL, NULL),
    (50005, 50005, 0, CURRENT_TIMESTAMP, 'cat-owner-e', 'author', 'Cat Owner E', NULL, NULL, NULL, NULL, NULL, NULL),
    (50006, 50006, 0, CURRENT_TIMESTAMP, 'cat-owner-f', 'author', 'Cat Owner F', NULL, NULL, NULL, NULL, NULL, NULL),
    (50007, 50007, 0, CURRENT_TIMESTAMP, 'cat-owner-g', 'author', 'Cat Owner G', NULL, NULL, NULL, NULL, NULL, NULL);

-- scm_repo  (stars differ so we can verify ordering)
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES
    (50001, 50001, 50001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'repo-feat-1',    'Featured lib 1',         NULL, 'mit', 'MIT License', 'main'),
    (50002, 50002, 50002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  80, 0, 'repo-feat-2',    'Featured lib 2',         NULL, 'mit', 'MIT License', 'main'),
    (50003, 50003, 50003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  60, 0, 'repo-grant-23',  'Grant winner 2023 lib',  NULL, 'mit', 'MIT License', 'main'),
    (50004, 50004, 50004, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  50, 0, 'repo-grant-24',  'Grant winner 2024 lib',  NULL, 'mit', 'MIT License', 'main'),
    (50005, 50005, 50005, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  40, 0, 'repo-compose',   'Compose UI lib',         NULL, 'mit', 'MIT License', 'main'),
    (50006, 50006, 50006, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  90, 0, 'repo-feat-comp', 'Featured + Compose lib', NULL, 'mit', 'MIT License', 'main'),
    (50007, 50007, 50007, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  45, 0, 'repo-grant-25',  'Grant winner 2025 lib',  NULL, 'mit', 'MIT License', 'main');

-- project
INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id)
VALUES
    (50001, 50001, CURRENT_TIMESTAMP, '1.0.0', 'Featured lib 1',         'repo-feat-1',    NULL, 50001),
    (50002, 50002, CURRENT_TIMESTAMP, '1.0.0', 'Featured lib 2',         'repo-feat-2',    NULL, 50002),
    (50003, 50003, CURRENT_TIMESTAMP, '1.0.0', 'Grant winner 2023 lib',  'repo-grant-23',  NULL, 50003),
    (50004, 50004, CURRENT_TIMESTAMP, '1.0.0', 'Grant winner 2024 lib',  'repo-grant-24',  NULL, 50004),
    (50005, 50005, CURRENT_TIMESTAMP, '1.0.0', 'Compose UI lib',         'repo-compose',   NULL, 50005),
    (50006, 50006, CURRENT_TIMESTAMP, '1.0.0', 'Featured + Compose lib', 'repo-feat-comp', NULL, 50006),
    (50007, 50007, CURRENT_TIMESTAMP, '1.0.0', 'Grant winner 2025 lib',  'repo-grant-25',  NULL, 50007);

-- package (one per project, version must match project.latest_version)
INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses)
VALUES
    (51001, 50001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'feat-1',    '1.0.0',  'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51002, 50002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'feat-2',    '1.0.0',  'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51003, 50003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'grant-23',  '1.0.0',  'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51004, 50004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'grant-24',  '1.0.0',  'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51005, 50005, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'compose-1', '1.0.0',  'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51006, 50006, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'feat-comp', '1.0.0',  'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51007, 50007, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'grant-25',  '1.0.0',  'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]');

-- package_target (at least one per package for project_index aggregation)
INSERT INTO public.package_target (package_id, platform, target)
VALUES
    (51001, 'JVM', NULL),
    (51002, 'JVM', NULL),
    (51003, 'JVM', NULL),
    (51004, 'JVM', NULL),
    (51005, 'JVM', NULL),
    (51006, 'JVM', NULL),
    (51007, 'JVM', NULL);

-- markers
INSERT INTO public.project_marker (project_id, type) VALUES
    (50001, 'FEATURED'),
    (50002, 'FEATURED'),
    (50003, 'GRANT_WINNER_2023'),
    (50004, 'GRANT_WINNER_2024'),
    (50005, 'COMPOSE_UI'),
    (50006, 'FEATURED'),
    (50006, 'COMPOSE_UI'),
    (50007, 'GRANT_WINNER_2025');
