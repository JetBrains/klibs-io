-- Seed data for testing project search filtering by markers

-- scm_owner
INSERT INTO public.scm_owner (
    id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company
) VALUES
    (20001, 20001, 0, CURRENT_TIMESTAMP, 'owner-a', 'author', 'Owner A', NULL, NULL, NULL, NULL, NULL, NULL),
    (20002, 20002, 0, CURRENT_TIMESTAMP, 'owner-b', 'author', 'Owner B', NULL, NULL, NULL, NULL, NULL, NULL),
    (20003, 20003, 0, CURRENT_TIMESTAMP, 'owner-c', 'author', 'Owner C', NULL, NULL, NULL, NULL, NULL, NULL);

-- scm_repo
INSERT INTO public.scm_repo (
    id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts,
    stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme
) VALUES
    (20001, 20001, 20001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 0, 'repo-a', 'Repo A', NULL, 'mit', 'MIT License', 'main', 'readme A'),
    (20002, 20002, 20002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 20, 0, 'repo-b', 'Repo B', NULL, 'mit', 'MIT License', 'main', 'readme B'),
    (20003, 20003, 20003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 30, 0, 'repo-c', 'Repo C', NULL, 'mit', 'MIT License', 'main', 'readme C');

-- project
INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description)
VALUES
    (30001, 20001, CURRENT_TIMESTAMP, '1.0.0', 'Project A'),
    (30002, 20002, CURRENT_TIMESTAMP, '1.0.0', 'Project B'),
    (30003, 20003, CURRENT_TIMESTAMP, '1.0.0', 'Project C');

-- package (latest version should match project.latest_version)
INSERT INTO public.package (
    id, project_id, release_ts, created_at, group_id, artifact_id, version, name, description,
    url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses
) VALUES
    (31001, 30001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-a', '1.0.0', 'lib-a', 'desc A', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (31002, 30002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-b', '1.0.0', 'lib-b', 'desc B', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (31003, 30003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-c', '1.0.0', 'lib-c', 'desc C', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]');

-- at least one platform per package so project_index can aggregate
INSERT INTO public.package_target (package_id, platform, target) VALUES
    (31001, 'JVM', NULL),
    (31002, 'JVM', NULL),
    (31003, 'JVM', NULL);

-- markers
-- Project A: FEATURED only
INSERT INTO public.project_marker (project_id, type) VALUES (30001, 'FEATURED');

-- Project B: GRANT_WINNER_2024 only
INSERT INTO public.project_marker (project_id, type) VALUES (30002, 'GRANT_WINNER_2024');

-- Project C: both FEATURED and GRANT_WINNER_2024
INSERT INTO public.project_marker (project_id, type) VALUES (30003, 'FEATURED');
INSERT INTO public.project_marker (project_id, type) VALUES (30003, 'GRANT_WINNER_2024');
