INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage,
                              twitter_handle, email, location, company)
VALUES (49001, 49001, 0, CURRENT_TIMESTAMP, 'cashapp', 'organization', 'Cash App', NULL, NULL, NULL, NULL, NULL, NULL),
       (49002, 49002, 0, CURRENT_TIMESTAMP, 'other', 'author', 'Other', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.scm_repo (id, id_native, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage,
                             license_key, license_name, default_branch)
VALUES (49001, 49001, 49001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0,
        'zipline', NULL, NULL, 'mit', 'MIT License', 'main'),
       (49002, 49002, 49002, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0,
        'consumer', NULL, NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.project VALUES (49001, 49001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'zipline', NULL, 49001),
                                  (49002, 49002, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'consumer', NULL, 49002);

INSERT INTO public.maven_coordinate (id, group_id, artifact_id, version)
VALUES (49101, 'app.cash.zipline', 'runtime', '1.0.0'),
       (49102, 'io.github.suspect', 'runtime', '1.0.0'),
       (49103, 'app.cash.zipline', 'loader', '1.0.0'),
       (49104, 'io.github.suspect', 'loader', '1.0.0'),
       (49105, 'io.github.suspect', 'extra', '1.0.0'),
       (49106, 'app.cash.zipline', 'depended', '1.0.0'),
       (49107, 'io.github.suspect', 'depended', '1.0.0'),
       (49108, 'app.cash.zipline', 'core', '1.0.0'),
       (49109, 'io.github.renamed', 'core', '1.0.0'),
       (49110, 'app.cash.zipline', 'reviewed', '1.0.0'),
       (49111, 'io.github.suspect', 'reviewed', '1.0.0'),
       (49112, 'app.cash.zipline', 'net', '1.0.0'),
       (49113, 'io.github.flaky', 'net', '1.0.0'),
       (49114, 'io.github.suspect-a', 'dup', '1.0.0'),
       (49115, 'io.github.suspect-b', 'dup', '1.0.0'),
       (49201, 'org.other', 'consumer', '1.0.0');

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url,
                            build_tool, build_tool_version, kotlin_version, developers, configuration, licenses,
                            scraper_type, maven_coordinate_id)
VALUES (49101, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'runtime', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49101),
       (49102, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'runtime', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49102),
       (49103, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'loader', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49103),
       (49104, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'loader', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49104),
       (49105, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'extra', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49105),
       (49106, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'depended', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49106),
       (49107, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'depended', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49107),
       (49108, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'core', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49108),
       (49109, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.renamed', 'core', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49109),
       (49110, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'reviewed', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49110),
       (49111, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'reviewed', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49111),
       (49112, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'net', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49112),
       (49113, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.flaky', 'net', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49113),
       (49114, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect-a', 'dup', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49114),
       (49115, 49001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect-b', 'dup', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49115),
       (49201, 49002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.other', 'consumer', '1.0.0', NULL, NULL, 'gradle', '8.0',
        '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 49201);

INSERT INTO public.package_dependency (package_id, dep_maven_coordinate_id)
VALUES (49101, 49102),
       (49201, 49107);

INSERT INTO public.suspicious_package_candidate (project_id, artifact_id, group_id, status, notes, detected_at)
VALUES (49001, 'runtime', 'app.cash.zipline', 'RESOLVED', 'the original', CURRENT_TIMESTAMP),
       (49001, 'runtime', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'loader', 'app.cash.zipline', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'loader', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'depended', 'app.cash.zipline', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'depended', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'core', 'app.cash.zipline', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'core', 'io.github.renamed', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'reviewed', 'app.cash.zipline', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'reviewed', 'io.github.suspect', 'RESOLVED', 'kept by reviewer', CURRENT_TIMESTAMP),
       (49001, 'net', 'app.cash.zipline', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'net', 'io.github.flaky', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'dup', 'io.github.suspect-a', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (49001, 'dup', 'io.github.suspect-b', 'PENDING', NULL, CURRENT_TIMESTAMP);
