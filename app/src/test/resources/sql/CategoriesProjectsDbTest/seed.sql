


INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES
    (50001, 50001, 0, CURRENT_TIMESTAMP, 'cat-owner-a', 'author', 'Cat Owner A', NULL, NULL, NULL, NULL, NULL, NULL),
    (50002, 50002, 0, CURRENT_TIMESTAMP, 'cat-owner-b', 'author', 'Cat Owner B', NULL, NULL, NULL, NULL, NULL, NULL),
    (50003, 50003, 0, CURRENT_TIMESTAMP, 'cat-owner-c', 'author', 'Cat Owner C', NULL, NULL, NULL, NULL, NULL, NULL),
    (50004, 50004, 0, CURRENT_TIMESTAMP, 'cat-owner-d', 'author', 'Cat Owner D', NULL, NULL, NULL, NULL, NULL, NULL),
    (50005, 50005, 0, CURRENT_TIMESTAMP, 'cat-owner-e', 'author', 'Cat Owner E', NULL, NULL, NULL, NULL, NULL, NULL),
    (50006, 50006, 0, CURRENT_TIMESTAMP, 'cat-owner-f', 'author', 'Cat Owner F', NULL, NULL, NULL, NULL, NULL, NULL),
    (50007, 50007, 0, CURRENT_TIMESTAMP, 'cat-owner-g', 'author', 'Cat Owner G', NULL, NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES
    (50001, 50001, 50001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'repo-feat-1',    'Featured lib 1',         NULL, 'mit', 'MIT License', 'main'),
    (50002, 50002, 50002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  80, 0, 'repo-feat-2',    'Featured lib 2',         NULL, 'mit', 'MIT License', 'main'),
    (50003, 50003, 50003, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  60, 0, 'repo-grant-23',  'Grant winner 2023 lib',  NULL, 'mit', 'MIT License', 'main'),
    (50004, 50004, 50004, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  50, 0, 'repo-grant-24',  'Grant winner 2024 lib',  NULL, 'mit', 'MIT License', 'main'),
    (50005, 50005, 50005, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  40, 0, 'repo-compose',   'Compose UI lib',         NULL, 'mit', 'MIT License', 'main'),
    (50006, 50006, 50006, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  90, 0, 'repo-feat-comp', 'Featured + Compose lib', NULL, 'mit', 'MIT License', 'main'),
    (50007, 50007, 50007, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  45, 0, 'repo-grant-25',  'Grant winner 2025 lib',  NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id)
VALUES
    (50001, 50001, CURRENT_TIMESTAMP, '1.0.0', 'Featured lib 1',         'repo-feat-1',    NULL, 50001),
    (50002, 50002, CURRENT_TIMESTAMP, '1.0.0', 'Featured lib 2',         'repo-feat-2',    NULL, 50002),
    (50003, 50003, CURRENT_TIMESTAMP, '1.0.0', 'Grant winner 2023 lib',  'repo-grant-23',  NULL, 50003),
    (50004, 50004, CURRENT_TIMESTAMP, '1.0.0', 'Grant winner 2024 lib',  'repo-grant-24',  NULL, 50004),
    (50005, 50005, CURRENT_TIMESTAMP, '1.0.0', 'Compose UI lib',         'repo-compose',   NULL, 50005),
    (50006, 50006, CURRENT_TIMESTAMP, '1.0.0', 'Featured + Compose lib', 'repo-feat-comp', NULL, 50006),
    (50007, 50007, CURRENT_TIMESTAMP, '1.0.0', 'Grant winner 2025 lib',  'repo-grant-25',  NULL, 50007);



INSERT INTO public.maven_coordinate (id, group_id, artifact_id, version) VALUES
    (1013010679, 'com.cat', 'feat-1', '1.0.0'),
    (1001362694, 'com.cat', 'feat-2', '1.0.0'),
    (1099880812, 'com.cat', 'grant-23', '1.0.0'),
    (1030641991, 'com.cat', 'grant-24', '1.0.0'),
    (1057327841, 'com.cat', 'compose-1', '1.0.0'),
    (1088238025, 'com.cat', 'feat-comp', '1.0.0'),
    (1068060872, 'com.cat', 'grant-25', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, maven_coordinate_id) VALUES (51001, 50001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'feat-1', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010679),
       (51002, 50002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'feat-2', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1001362694),
       (51003, 50003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'grant-23', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1099880812),
       (51004, 50004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'grant-24', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1030641991),
       (51005, 50005, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'compose-1', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1057327841),
       (51006, 50006, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'feat-comp', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1088238025),
       (51007, 50007, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.cat', 'grant-25', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1068060872);


INSERT INTO public.package_target (package_id, platform, target)
VALUES
    (51001, 'JVM', NULL),
    (51002, 'JVM', NULL),
    (51003, 'JVM', NULL),
    (51004, 'JVM', NULL),
    (51005, 'JVM', NULL),
    (51006, 'JVM', NULL),
    (51007, 'JVM', NULL);


INSERT INTO public.project_marker (project_id, type) VALUES
    (50001, 'FEATURED_DEPRECATED'),
    (50002, 'FEATURED_DEPRECATED'),
    (50003, 'GRANT_WINNER_2023'),
    (50004, 'GRANT_WINNER_2024'),
    (50005, 'COMPOSE_UI'),
    (50006, 'FEATURED_DEPRECATED'),
    (50006, 'COMPOSE_UI'),
    (50007, 'GRANT_WINNER_2025');

INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name)
VALUES
    (50008, 50008, 0, CURRENT_TIMESTAMP, 'Kotlin', 'author', 'Kotlin'),
    (50009, 50009, 0, CURRENT_TIMESTAMP, 'androidx', 'author', 'AndroidX'),
    (50010, 50010, 0, CURRENT_TIMESTAMP, 'JetBrains', 'author', 'JetBrains'),
    (50011, 50011, 0, CURRENT_TIMESTAMP, 'Google', 'author', 'Google'),
    (50012, 50012, 0, CURRENT_TIMESTAMP, 'Kotlin-Org', 'author', 'Kotlin'),
    (50013, 50013, 0, CURRENT_TIMESTAMP, 'JetBrainsOrg', 'author', 'JetBrains');

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES
    (50008, 50008, 50008, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,   1, 0, 'kotlin-repo-one', 'Ecosystem project', NULL, 'mit', 'MIT License', 'main'),
    (50009, 50009, 50008, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 200, 0, 'kotlin-repo-two', 'Ecosystem project', NULL, 'mit', 'MIT License', 'main'),
    (50010, 50010, 50009, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 100, 0, 'androidx-repo-one', 'Ecosystem project', NULL, 'mit', 'MIT License', 'main'),
    (50011, 50011, 50010, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,   2, 0, 'jetbrains-repo-one', 'Ecosystem project', NULL, 'mit', 'MIT License', 'main'),
    (50012, 50012, 50009, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,   3, 0, 'androidx-repo-two', 'Ecosystem project', NULL, 'mit', 'MIT License', 'main'),
    (50013, 50013, 50010, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,  20, 0, 'jetbrains-repo-two', 'Ecosystem project', NULL, 'mit', 'MIT License', 'main'),
    (50014, 50014, 50011, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 999, 0, 'androidx', 'Kotlin and JetBrains lookalike', NULL, 'mit', 'MIT License', 'main'),
    (50015, 50015, 50012, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 998, 0, 'kotlinx-coroutines', 'Kotlin lookalike owner', NULL, 'mit', 'MIT License', 'main'),
    (50016, 50016, 50013, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 997, 0, 'jetbrains-looking-repo', 'JetBrains lookalike owner', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id)
VALUES
    (50008, 50008, CURRENT_TIMESTAMP, '1.0.0', 'Kotlin ecosystem project', 'kotlin-one', NULL, 50008),
    (50009, 50009, CURRENT_TIMESTAMP, '1.0.0', 'Kotlin ecosystem project', 'kotlin-two', NULL, 50008),
    (50010, 50010, CURRENT_TIMESTAMP, '1.0.0', 'AndroidX ecosystem project', 'androidx-one', NULL, 50009),
    (50011, 50011, CURRENT_TIMESTAMP, '1.0.0', 'JetBrains ecosystem project', 'jetbrains-one', NULL, 50010),
    (50012, 50012, CURRENT_TIMESTAMP, '1.0.0', 'AndroidX ecosystem project', 'androidx-two', NULL, 50009),
    (50013, 50013, CURRENT_TIMESTAMP, '1.0.0', 'JetBrains ecosystem project', 'jetbrains-two', NULL, 50010),
    (50014, 50014, CURRENT_TIMESTAMP, '1.0.0', 'Kotlin project from Google', 'Kotlin', NULL, 50011),
    (50015, 50015, CURRENT_TIMESTAMP, '1.0.0', 'Kotlin lookalike project', 'kotlin-coroutines', NULL, 50012),
    (50016, 50016, CURRENT_TIMESTAMP, '1.0.0', 'JetBrains lookalike project', 'jetbrains-library', NULL, 50013);

UPDATE public.project
SET dependent_count = CASE id
    WHEN 50008 THEN 30
    WHEN 50009 THEN 0
    WHEN 50010 THEN 30
    WHEN 50011 THEN 20
    WHEN 50012 THEN 0
    WHEN 50013 THEN 20
    WHEN 50014 THEN 1000
    WHEN 50015 THEN 2000
    WHEN 50016 THEN 3000
END
WHERE id BETWEEN 50008 AND 50016;

INSERT INTO public.project_marker (project_id, type)
VALUES (50008, 'CATEGORY_SELECTOR_TEST');

INSERT INTO public.project_tags (project_id, origin, value)
VALUES (50008, 'USER', 'category-test-tag');

INSERT INTO public.maven_coordinate (id, group_id, artifact_id, version) VALUES
    (1013010680, 'io.category', 'kotlin-one', '1.0.0'),
    (1013010681, 'io.category', 'kotlin-two', '1.0.0'),
    (1013010682, 'io.category', 'androidx-one', '1.0.0'),
    (1013010683, 'io.category', 'jetbrains-one', '1.0.0'),
    (1013010684, 'io.category', 'androidx-two', '1.0.0'),
    (1013010685, 'io.category', 'jetbrains-two', '1.0.0'),
    (1013010686, 'org.jetbrains.kotlin', 'androidx', '1.0.0'),
    (1013010687, 'org.jetbrains.kotlinx', 'kotlin-coroutines', '1.0.0'),
    (1013010688, 'com.jetbrains', 'jetbrains-library', '1.0.0');

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, maven_coordinate_id) VALUES
    (51008, 50008, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.category', 'kotlin-one', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010680),
    (51009, 50009, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.category', 'kotlin-two', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010681),
    (51010, 50010, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.category', 'androidx-one', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010682),
    (51011, 50011, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.category', 'jetbrains-one', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010683),
    (51012, 50012, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.category', 'androidx-two', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010684),
    (51013, 50013, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.category', 'jetbrains-two', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010685),
    (51014, 50014, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.jetbrains.kotlin', 'androidx', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010686),
    (51015, 50015, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.jetbrains.kotlinx', 'kotlin-coroutines', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010687),
    (51016, 50016, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.jetbrains', 'jetbrains-library', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010688);

INSERT INTO public.package_target (package_id, platform, target)
VALUES
    (51008, 'JVM', '17'),
    (51009, 'JVM', NULL),
    (51010, 'JVM', NULL),
    (51011, 'JVM', NULL),
    (51012, 'JVM', NULL),
    (51013, 'JVM', NULL),
    (51014, 'JVM', NULL),
    (51015, 'JVM', NULL),
    (51016, 'JVM', NULL);

INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES
    (50017, 50017, 50008, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 4000, 0, 'hidden-kotlin-repo', 'Hidden ecosystem project', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id)
VALUES
    (50017, 50017, CURRENT_TIMESTAMP, '1.0.0', 'Hidden Kotlin ecosystem project', 'hidden-kotlin-project', NULL, 50008);

UPDATE public.project SET dependent_count = 4000 WHERE id = 50017;

INSERT INTO public.maven_coordinate (id, group_id, artifact_id, version)
VALUES (1013010689, 'io.category', 'hidden-kotlin-project', '1.0.0');

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, maven_coordinate_id)
VALUES (51017, 50017, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.category', 'hidden-kotlin-project', '1.0.0', 'desc', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1013010689);

INSERT INTO public.package_target (package_id, platform, target)
VALUES (51017, 'JVM', NULL);

INSERT INTO public.project_hidden (project_id, hidden_at, origin, reason)
VALUES (50017, CURRENT_TIMESTAMP, 'AUTO', 'Hidden category test fixture');
