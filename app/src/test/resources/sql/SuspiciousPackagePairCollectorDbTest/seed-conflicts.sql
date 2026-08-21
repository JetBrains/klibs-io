-- One project (9001) whose artifact `conflict-lib` is published under two group ids
-- (io.github.alice with 2 versions, com.example with 1) -> a within-project conflict.
-- Plus two decoys that must NOT surface: `solo-lib` (single group) and `orphan` (project_id NULL).
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
    -- conflict-lib under io.github.alice (2 versions, distinct release timestamps)
    (8001, 9001, 'SEARCH_MAVEN', 'io.github.alice', 'conflict-lib', '1.0.0', TIMESTAMP '2024-01-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7001),
    (8002, 9001, 'SEARCH_MAVEN', 'io.github.alice', 'conflict-lib', '2.0.0', TIMESTAMP '2024-06-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7002),
    -- conflict-lib under com.example (1 version) -> cross-group conflict, owner not derivable
    (8003, 9001, 'SEARCH_MAVEN', 'com.example', 'conflict-lib', '1.0.0', TIMESTAMP '2024-03-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7003),
    -- solo-lib under a single group -> must be ignored
    (8004, 9001, 'SEARCH_MAVEN', 'io.github.alice', 'solo-lib', '1.0.0', TIMESTAMP '2024-01-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7004),
    -- orphan under two groups but project_id NULL -> must be skipped
    (8005, NULL, 'SEARCH_MAVEN', 'io.github.x', 'orphan', '1.0.0', TIMESTAMP '2024-01-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7005),
    (8006, NULL, 'SEARCH_MAVEN', 'io.github.y', 'orphan', '1.0.0', TIMESTAMP '2024-01-01 00:00:00',
     NULL, NULL, NULL, 'gradle', '8.0', '2.0.0', '[]'::jsonb, '[]'::jsonb, NULL, false, 7006);
