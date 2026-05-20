-- Fixture for ProjectRepositoryJdbc.recomputeAllDependentCounts.
--
-- Layout:
--   project A (id=9101) — depended on by project B's package(s).
--   project B (id=9102) — has a package that depends on:
--                          * project A's GAV (counts toward A.dependent_count)
--                          * a third-party artifact unknown to klibs (no project — must not count)
--                          * itself (must be excluded by the FILTER)
--   project C (id=9103) — has no dependencies at all (dependent_count must stay 0).
--
-- Expected outcome after recomputeAllDependentCounts():
--   A.dependent_count = 1 (B depends on A)
--   B.dependent_count = 0 (only self-dependency)
--   C.dependent_count = 0 (no dependencies)

-- ---- scm owners / repos ----
INSERT INTO scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage,
                       twitter_handle, email, location, company)
VALUES (9101, 9101, 0, CURRENT_TIMESTAMP, 'klibs-dep-test', 'author',
        'klibs dep test', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO scm_repo (id, id_native, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                      updated_at, last_activity_ts, stars, open_issues, name, description, homepage,
                      license_key, license_name, default_branch)
VALUES (9101, 9101, 9101, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
        0, 0, 'repo-a', NULL, NULL, 'mit', 'MIT License', 'main'),
       (9102, 9102, 9101, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
        0, 0, 'repo-b', NULL, NULL, 'mit', 'MIT License', 'main'),
       (9103, 9103, 9101, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
        0, 0, 'repo-c', NULL, NULL, 'mit', 'MIT License', 'main');

-- ---- projects ----
INSERT INTO project (id, scm_repo_id, owner_id, name, description, minimized_readme,
                     latest_version, latest_version_ts, dependent_count)
VALUES (9101, 9101, 9101, 'project-a', NULL, NULL, '1.0.0', CURRENT_TIMESTAMP, 0),
       (9102, 9102, 9101, 'project-b', NULL, NULL, '1.0.0', CURRENT_TIMESTAMP, 0),
       (9103, 9103, 9101, 'project-c', NULL, NULL, '1.0.0', CURRENT_TIMESTAMP, 0);

-- ---- maven_artifact rows ----
-- Pre-seed every (g,a,v) we touch — the package rows below reference these via the
-- NOT NULL package.maven_artifact_id FK introduced in 2026-04-26_*.yml.
-- 9201 = io.klibs.test:lib-a:1.0.0  (A's coords — A publishes it; B depends on it)
-- 9202 = io.klibs.test:lib-b:1.0.0  (B's coords — B publishes it; B's self-dependency)
-- 9203 = com.external:unknown:9.9.9 (third-party, no klibs project)
-- 9204 = io.klibs.test:lib-c:1.0.0  (C's coords — C publishes it; nobody depends on it)
INSERT INTO maven_artifact (id, group_id, artifact_id, version)
VALUES (9201, 'io.klibs.test', 'lib-a', '1.0.0'),
       (9202, 'io.klibs.test', 'lib-b', '1.0.0'),
       (9203, 'com.external', 'unknown', '9.9.9'),
       (9204, 'io.klibs.test', 'lib-c', '1.0.0');

-- ---- packages ----
-- Project A publishes io.klibs.test:lib-a:1.0.0.
INSERT INTO package (id, project_id, scraper_type, group_id, artifact_id, version, release_ts,
                     description, url, scm_url, build_tool, build_tool_version, kotlin_version,
                     developers, licenses, configuration, generated_description, maven_artifact_id)
VALUES (9101, 9101, 'SEARCH_MAVEN', 'io.klibs.test', 'lib-a', '1.0.0', CURRENT_TIMESTAMP,
        NULL, NULL, NULL, 'gradle', '8.0', '2.0.0',
        '[]'::jsonb, '[]'::jsonb, NULL, false, 9201);

-- Project B publishes io.klibs.test:lib-b:1.0.0.
INSERT INTO package (id, project_id, scraper_type, group_id, artifact_id, version, release_ts,
                     description, url, scm_url, build_tool, build_tool_version, kotlin_version,
                     developers, licenses, configuration, generated_description, maven_artifact_id)
VALUES (9102, 9102, 'SEARCH_MAVEN', 'io.klibs.test', 'lib-b', '1.0.0', CURRENT_TIMESTAMP,
        NULL, NULL, NULL, 'gradle', '8.0', '2.0.0',
        '[]'::jsonb, '[]'::jsonb, NULL, false, 9202);

-- Project C publishes io.klibs.test:lib-c:1.0.0.
INSERT INTO package (id, project_id, scraper_type, group_id, artifact_id, version, release_ts,
                     description, url, scm_url, build_tool, build_tool_version, kotlin_version,
                     developers, licenses, configuration, generated_description, maven_artifact_id)
VALUES (9103, 9103, 'SEARCH_MAVEN', 'io.klibs.test', 'lib-c', '1.0.0', CURRENT_TIMESTAMP,
        NULL, NULL, NULL, 'gradle', '8.0', '2.0.0',
        '[]'::jsonb, '[]'::jsonb, NULL, false, 9204);

-- ---- dependencies ----
-- Project B's package depends on A, on itself, and on an unknown artifact.
INSERT INTO package_dependency (package_id, dep_maven_artifact_id)
VALUES (9102, 9201),
       (9102, 9202),
       (9102, 9203);
