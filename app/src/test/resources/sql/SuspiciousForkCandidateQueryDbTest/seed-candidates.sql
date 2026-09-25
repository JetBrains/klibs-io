INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage,
                              twitter_handle, email, location, company)
VALUES (48001, 48001, 0, CURRENT_TIMESTAMP, 'cashapp', 'organization', 'Cash App', NULL, NULL, NULL, NULL, NULL, NULL),
       (48002, 48002, 0, CURRENT_TIMESTAMP, 'other', 'author', 'Other', NULL, NULL, NULL, NULL, NULL, NULL),
       (48003, 48003, 0, CURRENT_TIMESTAMP, 'suspect', 'author', 'Suspect', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.scm_repo (id, id_native, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage,
                             license_key, license_name, default_branch)
VALUES (48001, 48001, 48001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0,
        'zipline', NULL, NULL, 'mit', 'MIT License', 'main'),
       (48002, 48002, 48002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0,
        'consumer', NULL, NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.project VALUES (48001, 48001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'zipline', NULL, 48003),
                                  (48002, 48002, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'consumer', NULL, 48002);

INSERT INTO public.maven_coordinate (id, group_id, artifact_id, version)
VALUES (48101, 'app.cash.zipline', 'qualifies', '1.0.0'),
       (48102, 'io.github.suspect', 'qualifies', '1.0.0'),
       (48103, 'app.cash.zipline', 'com-github', '1.0.0'),
       (48104, 'com.github.suspect', 'com-github', '1.0.0'),
       (48105, 'org.alpha', 'plain', '1.0.0'),
       (48106, 'org.beta', 'plain', '1.0.0'),
       (48107, 'app.cash.zipline', 'own-case', '1.0.0'),
       (48108, 'io.github.CashApp', 'own-case', '1.0.0'),
       (48109, 'app.cash.zipline', 'reviewed', '1.0.0'),
       (48110, 'io.github.suspect', 'reviewed', '1.0.0'),
       (48112, 'app.cash.zipline', 'depended', '1.0.0'),
       (48113, 'io.github.suspect', 'depended', '1.0.0'),
       (48114, 'app.cash.zipline', 'orphan-dep', '1.0.0'),
       (48115, 'io.github.suspect', 'orphan-dep', '1.0.0'),
       (48116, 'app.cash.zipline', 'sibling-dep', '1.0.0'),
       (48117, 'io.github.suspect', 'sibling-dep', '1.0.0'),
       (48118, 'app.cash.zipline', 'banned-exact', '1.0.0'),
       (48119, 'io.github.suspect', 'banned-exact', '1.0.0'),
       (48120, 'app.cash.zipline', 'banned-group', '1.0.0'),
       (48121, 'io.github.banned-owner', 'banned-group', '1.0.0'),
       (48222, 'org.other', 'consumer', '1.0.0'),
       (48323, 'org.orphan', 'consumer', '1.0.0');

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url,
                            build_tool, build_tool_version, kotlin_version, developers, configuration, licenses,
                            scraper_type, maven_coordinate_id)
VALUES (48101, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'qualifies', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48101),
       (48102, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'qualifies', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48102),
       (48103, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'com-github', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48103),
       (48104, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.github.suspect', 'com-github', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48104),
       (48105, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.alpha', 'plain', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48105),
       (48106, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.beta', 'plain', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48106),
       (48107, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'own-case', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48107),
       (48108, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.CashApp', 'own-case', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48108),
       (48109, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'reviewed', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48109),
       (48110, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'reviewed', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48110),
       (48112, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'depended', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48112),
       (48113, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'depended', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48113),
       (48114, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'orphan-dep', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48114),
       (48115, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'orphan-dep', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48115),
       (48116, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'sibling-dep', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48116),
       (48117, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'sibling-dep', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48117),
       (48118, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'banned-exact', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48118),
       (48119, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'banned-exact', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48119),
       (48120, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'banned-group', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48120),
       (48121, 48001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.banned-owner', 'banned-group', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48121),
       (48222, 48002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.other', 'consumer', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48222),
       (48323, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.orphan', 'consumer', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 48323);

INSERT INTO public.package_dependency (package_id, dep_maven_coordinate_id)
VALUES (48222, 48113),
       (48323, 48115),
       (48101, 48117);

INSERT INTO public.banned_packages (group_id, artifact_id, reason)
VALUES ('io.github.suspect', 'banned-exact', 'test'),
       ('io.github.banned-owner', NULL, 'test');

INSERT INTO public.suspicious_package_candidate (project_id, artifact_id, group_id, status, notes, detected_at)
VALUES (48001, 'qualifies', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (48001, 'com-github', 'com.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (48001, 'plain', 'org.alpha', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (48001, 'plain', 'org.beta', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (48001, 'own-case', 'io.github.CashApp', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (48001, 'reviewed', 'io.github.suspect', 'RESOLVED', 'kept by reviewer', CURRENT_TIMESTAMP),
       (48001, 'depended', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (48001, 'orphan-dep', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (48001, 'sibling-dep', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (48001, 'banned-exact', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (48001, 'banned-group', 'io.github.banned-owner', 'PENDING', NULL, CURRENT_TIMESTAMP);
