




INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1050552343, 'com.one', 'keep', '1.0.0'),
    (1092439885, 'com.one', 'del', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (8020, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.one', 'keep', '1.0.0', 'Keep', 'https://ex.com/keep', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1050552343);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8020, 'JVM', '1.8');


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (8021, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.one', 'del', '1.0.0', 'Del', 'https://ex.com/del', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1092439885);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8021, 'JVM', '1.8');
