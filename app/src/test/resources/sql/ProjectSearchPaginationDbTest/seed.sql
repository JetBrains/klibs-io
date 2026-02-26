-- Seed data for testing project search pagination stability.
-- All projects share the same star count to expose non-deterministic ordering.

-- scm_owner
INSERT INTO public.scm_owner (
    id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company
) VALUES
    (40001, 40001, 0, CURRENT_TIMESTAMP, 'pagination-owner', 'author', 'Pagination Owner', NULL, NULL, NULL, NULL, NULL, NULL);

-- scm_repo — all repos have the same star count (100)
INSERT INTO public.scm_repo (
    id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts,
    stars, open_issues, name, description, homepage, license_key, license_name, default_branch
) VALUES
    (40001, 40001, 40001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'repo-p1', 'Repo P1', NULL, 'mit', 'MIT License', 'main'),
    (40002, 40002, 40001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'repo-p2', 'Repo P2', NULL, 'mit', 'MIT License', 'main'),
    (40003, 40003, 40001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'repo-p3', 'Repo P3', NULL, 'mit', 'MIT License', 'main'),
    (40004, 40004, 40001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'repo-p4', 'Repo P4', NULL, 'mit', 'MIT License', 'main'),
    (40005, 40005, 40001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'repo-p5', 'Repo P5', NULL, 'mit', 'MIT License', 'main'),
    (40006, 40006, 40001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'repo-p6', 'Repo P6', NULL, 'mit', 'MIT License', 'main');

-- project
INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id) VALUES
    (50001, 40001, CURRENT_TIMESTAMP, '1.0.0', 'Project P1', 'repo-p1', NULL, 40001),
    (50002, 40002, CURRENT_TIMESTAMP, '1.0.0', 'Project P2', 'repo-p2', NULL, 40001),
    (50003, 40003, CURRENT_TIMESTAMP, '1.0.0', 'Project P3', 'repo-p3', NULL, 40001),
    (50004, 40004, CURRENT_TIMESTAMP, '1.0.0', 'Project P4', 'repo-p4', NULL, 40001),
    (50005, 40005, CURRENT_TIMESTAMP, '1.0.0', 'Project P5', 'repo-p5', NULL, 40001),
    (50006, 40006, CURRENT_TIMESTAMP, '1.0.0', 'Project P6', 'repo-p6', NULL, 40001);

-- package (one per project)
INSERT INTO public.package (
    id, project_id, release_ts, created_at, group_id, artifact_id, version, name, description,
    url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses
) VALUES
    (51001, 50001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.pagination', 'lib-p1', '1.0.0', 'lib-p1', 'desc P1', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51002, 50002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.pagination', 'lib-p2', '1.0.0', 'lib-p2', 'desc P2', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51003, 50003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.pagination', 'lib-p3', '1.0.0', 'lib-p3', 'desc P3', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51004, 50004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.pagination', 'lib-p4', '1.0.0', 'lib-p4', 'desc P4', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51005, 50005, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.pagination', 'lib-p5', '1.0.0', 'lib-p5', 'desc P5', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]'),
    (51006, 50006, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.pagination', 'lib-p6', '1.0.0', 'lib-p6', 'desc P6', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]');

-- package_target (one JVM platform per package)
INSERT INTO public.package_target (package_id, platform, target) VALUES
    (51001, 'JVM', NULL),
    (51002, 'JVM', NULL),
    (51003, 'JVM', NULL),
    (51004, 'JVM', NULL),
    (51005, 'JVM', NULL),
    (51006, 'JVM', NULL);
