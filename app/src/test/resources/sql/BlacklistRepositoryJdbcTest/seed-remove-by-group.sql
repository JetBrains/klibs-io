





INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1079843656, 'com.rm', 'a', '1.0.0'),
    (1014926676, 'com.rm', 'b', '1.0.0'),
    (1041240143, 'com.keep', 'c', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (8010, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.rm', 'a', '1.0.0', 'A', 'https://ex.com/a', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1079843656);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8010, 'JVM', '1.8');


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (8011, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.rm', 'b', '1.0.0', 'B', 'https://ex.com/b', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1014926676);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8011, 'JVM', '1.8');


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (8012, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.keep', 'c', '1.0.0', 'C', 'https://ex.com/c', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1041240143);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8012, 'JVM', '1.8');
