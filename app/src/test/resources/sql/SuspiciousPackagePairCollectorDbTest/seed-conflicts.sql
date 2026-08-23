-- `conflict-lib` (two group ids in project 9001) must appear;
-- `solo-lib` (single group) and `orphan` (project_id NULL) must not.
INSERT INTO scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage,
                       twitter_handle, email, location, company)
VALUES (9001, 9001, 0, CURRENT_TIMESTAMP, 'klibs-test-owner', 'author',
        'klibs test owner', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO project (id, scm_repo_id, owner_id, name, description, minimized_readme,
                     latest_version, latest_version_ts, dependent_count)
VALUES (9001, NULL, 9001, 'test-project', NULL, NULL, '1.0.0', CURRENT_TIMESTAMP, 0);

INSERT INTO maven_artifact (id, group_id, artifact_id, version) VALUES
    (7001, 'io.github.alice', 'conflict-lib', '1.0.0'),
    (7002, 'io.github.alice', 'conflict-lib', '2.0.0'),
    (7003, 'com.example',     'conflict-lib', '1.0.0'),
    (7004, 'io.github.alice', 'solo-lib',     '1.0.0'),
    (7005, 'io.github.x',     'orphan',       '1.0.0'),
    (7006, 'io.github.y',     'orphan',       '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO package (id, project_id, scraper_type, group_id, artifact_id, version, release_ts,
                     description, url, scm_url, build_tool, build_tool_version, kotlin_version,
                     developers, licenses, configuration, generated_description, maven_artifact_id) VALUES
    (8001, 9001, 'SEARCH_MAVEN', 'io.github.alice', 'conflict-lib', '1.0.0', TIMESTAMP '2024-01-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7001),
    (8002, 9001, 'SEARCH_MAVEN', 'io.github.alice', 'conflict-lib', '2.0.0', TIMESTAMP '2024-06-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7002),
    (8003, 9001, 'SEARCH_MAVEN', 'com.example', 'conflict-lib', '1.0.0', TIMESTAMP '2024-03-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7003),
    (8004, 9001, 'SEARCH_MAVEN', 'io.github.alice', 'solo-lib', '1.0.0', TIMESTAMP '2024-01-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7004),
    (8005, NULL, 'SEARCH_MAVEN', 'io.github.x', 'orphan', '1.0.0', TIMESTAMP '2024-01-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7005),
    (8006, NULL, 'SEARCH_MAVEN', 'io.github.y', 'orphan', '1.0.0', TIMESTAMP '2024-01-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7006);
