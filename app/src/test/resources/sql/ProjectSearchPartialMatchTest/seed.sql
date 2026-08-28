-- Libraries whose names exercise partial matching: a compound name to match by head and by tail,
-- and a camelCase name, which indexes as one token far longer than the 18-char max_gram.
INSERT INTO public.scm_owner (
    id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company
) VALUES
    (62001, 62001, 0, CURRENT_TIMESTAMP, 'sqldelight', 'organization', 'SQLDelight', NULL, NULL, NULL, NULL, NULL, NULL),
    (62002, 62002, 0, CURRENT_TIMESTAMP, 'InsertKoinIO', 'organization', 'Koin', NULL, NULL, NULL, NULL, NULL, NULL),
    (62003, 62003, 0, CURRENT_TIMESTAMP, 'russhwolf', 'author', 'Russell Wolf', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.scm_repo (
    id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts,
    stars, open_issues, name, description, homepage, license_key, license_name, default_branch
) VALUES
    (62001, 62001, 62001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 50, 0, 'sqldelight', 'Typesafe SQL for Kotlin', NULL, 'apache-2.0', 'Apache License 2.0', 'main'),
    (62002, 62002, 62002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'koin', 'Kotlin dependency injection', NULL, 'apache-2.0', 'Apache License 2.0', 'main'),
    (62003, 62003, 62003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 30, 0, 'MultiplatformSettingsDataStore', 'Key-value storage for KMP', NULL, 'apache-2.0', 'Apache License 2.0', 'main');

INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id) VALUES
    (73001, 62001, CURRENT_TIMESTAMP, '1.0.0', 'Typesafe SQL for Kotlin', 'sqldelight', 'readme', 62001),
    (73002, 62002, CURRENT_TIMESTAMP, '1.0.0', 'Kotlin dependency injection', 'koin', 'readme', 62002),
    (73003, 62003, CURRENT_TIMESTAMP, '1.0.0', 'Key-value storage for KMP', 'MultiplatformSettingsDataStore', 'readme', 62003);

INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1220000001, 'app.cash.sqldelight', 'runtime', '1.0.0'),
    (1220000002, 'io.insert-koin', 'koin-core', '1.0.0'),
    (1220000003, 'com.russhwolf', 'multiplatform-settings-datastore', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, maven_artifact_id) VALUES
    (74001, 73001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.sqldelight', 'runtime', '1.0.0', 'SQLDelight runtime', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"Apache License 2.0"}]', 1220000001),
    (74002, 73002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.insert-koin', 'koin-core', '1.0.0', 'Koin core', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"Apache License 2.0"}]', 1220000002),
    (74003, 73003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.russhwolf', 'multiplatform-settings-datastore', '1.0.0', 'Multiplatform Settings', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"Apache License 2.0"}]', 1220000003);

INSERT INTO public.package_target (package_id, platform, target) VALUES
    (74001, 'JVM', NULL),
    (74002, 'JVM', NULL),
    (74003, 'JVM', NULL);
