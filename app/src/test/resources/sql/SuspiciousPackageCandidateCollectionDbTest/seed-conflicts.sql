-- 47001: two groupIds (one published once). 47002: three groupIds. 47003: one groupId, three
-- versions. Plus two packages with no project. Expected after a collection run: 5 candidates.

INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage,
                              twitter_handle, email, location, company)
VALUES (47001, 47001, 0, CURRENT_TIMESTAMP, 'owner-47001', 'author', 'Owner 47001', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.scm_repo (id, id_native, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage,
                             license_key, license_name, default_branch)
VALUES (47001, 47001, 47001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0,
        'repo-47001', NULL, NULL, 'mit', 'MIT License', 'main'),
       (47002, 47002, 47001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0,
        'repo-47002', NULL, NULL, 'mit', 'MIT License', 'main'),
       (47003, 47003, 47001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0,
        'repo-47003', NULL, NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.project VALUES (47001, 47001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'repo-47001', NULL, 47001),
                                  (47002, 47002, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'repo-47002', NULL, 47001),
                                  (47003, 47003, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'repo-47003', NULL, 47001);

INSERT INTO public.maven_artifact (id, group_id, artifact_id, version)
VALUES (47101, 'org.alpha', 'lib', '1.0.0'),
       (47102, 'org.alpha', 'lib', '2.0.0'),
       (47103, 'io.github.beta', 'lib', '1.0.0'),
       (47201, 'org.gamma', 'tool', '1.0.0'),
       (47202, 'org.delta', 'tool', '1.0.0'),
       (47203, 'org.epsilon', 'tool', '1.0.0'),
       (47301, 'org.zeta', 'solo', '1.0.0'),
       (47302, 'org.zeta', 'solo', '2.0.0'),
       (47303, 'org.zeta', 'solo', '3.0.0'),
       (47401, 'org.orphan.one', 'stray', '1.0.0'),
       (47402, 'org.orphan.two', 'stray', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url,
                            build_tool, build_tool_version, kotlin_version, developers, configuration, licenses,
                            scraper_type, maven_artifact_id)
VALUES (47101, 47001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.alpha', 'lib', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47101),
       (47102, 47001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.alpha', 'lib', '2.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47102),
       (47103, 47001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.beta', 'lib', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47103),
       (47201, 47002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.gamma', 'tool', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47201),
       (47202, 47002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.delta', 'tool', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47202),
       (47203, 47002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.epsilon', 'tool', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47203),
       (47301, 47003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.zeta', 'solo', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47301),
       (47302, 47003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.zeta', 'solo', '2.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47302),
       (47303, 47003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.zeta', 'solo', '3.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47303),
       (47401, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.orphan.one', 'stray', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47401),
       (47402, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.orphan.two', 'stray', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 47402);
