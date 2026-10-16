INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage,
                              twitter_handle, email, location, company)
VALUES (60001, 60001, 0, CURRENT_TIMESTAMP, 'cashapp', 'organization', 'Cash App', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.scm_repo (id, id_native, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage,
                             license_key, license_name, default_branch)
VALUES (60001, 60001, 60001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0,
        'zipline', NULL, NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.project VALUES (60001, 60001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'zipline', NULL, 60001);

INSERT INTO public.maven_coordinate (id, group_id, artifact_id, version)
VALUES (60101, 'app.cash.zipline', 'loader', '1.0.0'),
       (60102, 'io.github.suspect', 'loader', '1.0.0'),
       (60103, 'io.github.suspect', 'runtime', '1.0.0');

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url,
                            build_tool, build_tool_version, kotlin_version, developers, configuration, licenses,
                            scraper_type, maven_coordinate_id)
VALUES (60101, 60001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'app.cash.zipline', 'loader', '1.0.0', NULL, NULL, 'gradle',
        '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 60101),
       (60102, 60001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'loader', '1.0.0', NULL, NULL, 'gradle',
        '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 60102),
       (60103, 60001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.github.suspect', 'runtime', '1.0.0', NULL, NULL, 'gradle',
        '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 60103);

INSERT INTO public.suspicious_package_candidate (project_id, artifact_id, group_id, status, notes, detected_at)
VALUES (60001, 'loader', 'app.cash.zipline', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (60001, 'loader', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (60001, 'runtime', 'io.github.suspect', 'PENDING', NULL, CURRENT_TIMESTAMP),
       (60001, 'reviewed', 'io.github.suspect', 'RESOLVED', 'kept by reviewer', CURRENT_TIMESTAMP);
