


INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1075077997, 'com.example', 'lib-a', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (8001, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'com.example', 'lib-a', '1.0.0', 'Example A', 'https://example.com/lib-a', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1075077997);
