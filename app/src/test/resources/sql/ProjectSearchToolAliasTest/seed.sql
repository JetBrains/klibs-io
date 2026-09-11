-- The KMP equivalents two aliases point at (koin, sqldelight) plus androidx/room, which the
-- `room alternative` alias must not displace for a plain `room` query.
INSERT INTO public.scm_owner (
    id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company
) VALUES
    (61001, 61001, 0, CURRENT_TIMESTAMP, 'InsertKoinIO', 'organization', 'Koin', NULL, NULL, NULL, NULL, NULL, NULL),
    (61002, 61002, 0, CURRENT_TIMESTAMP, 'androidx', 'organization', 'AndroidX', NULL, NULL, NULL, NULL, NULL, NULL),
    (61003, 61003, 0, CURRENT_TIMESTAMP, 'sqldelight', 'organization', 'SQLDelight', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.scm_repo (
    id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts,
    stars, open_issues, name, description, homepage, license_key, license_name, default_branch
) VALUES
    (61001, 61001, 61001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'koin', 'Kotlin dependency injection', NULL, 'apache-2.0', 'Apache License 2.0', 'main'),
    (61002, 61002, 61002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 900, 0, 'room', 'Persistence library', NULL, 'apache-2.0', 'Apache License 2.0', 'main'),
    (61003, 61003, 61003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 50, 0, 'sqldelight', 'Typesafe SQL for Kotlin', NULL, 'apache-2.0', 'Apache License 2.0', 'main');

INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id) VALUES
    (71001, 61001, CURRENT_TIMESTAMP, '1.0.0', 'Kotlin dependency injection', 'koin', 'readme', 61001),
    (71002, 61002, CURRENT_TIMESTAMP, '1.0.0', 'Persistence library', 'room', 'readme', 61002),
    (71003, 61003, CURRENT_TIMESTAMP, '1.0.0', 'Typesafe SQL for Kotlin', 'sqldelight', 'readme', 61003);

INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1210000001, 'io.insert-koin', 'koin-core', '1.0.0'),
    (1210000002, 'androidx.room', 'room-runtime', '1.0.0'),
    (1210000003, 'app.cash.sqldelight', 'runtime', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, maven_artifact_id) VALUES
    (72001, 71001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.insert-koin', 'koin-core', '1.0.0', 'Koin core', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"Apache License 2.0"}]', 1210000001),
    (72002, 71002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'androidx.room', 'room-runtime', '1.0.0', 'Room runtime', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"Apache License 2.0"}]', 1210000002),
    (72003, 71003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.sqldelight', 'runtime', '1.0.0', 'SQLDelight runtime', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"Apache License 2.0"}]', 1210000003);

INSERT INTO public.package_target (package_id, platform, target) VALUES
    (72001, 'JVM', NULL),
    (72002, 'JVM', NULL),
    (72003, 'JVM', NULL);
