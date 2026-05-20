
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (9001, 9001, 0, CURRENT_TIMESTAMP, 'owner-9001', 'author', 'Owner 9001', 'Owner desc', NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (9001, 9001, 9001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-9001', 'Repo 9001', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project VALUES (9001, 9001, CURRENT_TIMESTAMP, '2.0.0', CURRENT_TIMESTAMP, 'repo-9001', NULL, 9001);




INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1061382813, 'org.example', 'libA', '1.0.0'),
    (1019966004, 'org.example', 'libA', '2.0.0'),
    (1038860513, 'org.example', 'libB', '3.1.4')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (9002, 9001, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP - INTERVAL '1 year', 'org.example', 'libA', '1.0.0', 'Old A', 'https://example.com/libA', 'gradle', '7.0', '1.9.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1061382813);


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (9003, 9001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'libA', '2.0.0', 'New A', 'https://example.com/libA', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1019966004);


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (9004, 9001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'libB', '3.1.4', 'Lib B', 'https://example.com/libB', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1038860513);


INSERT INTO public.package_target (package_id, platform, target) VALUES (9002, 'JS', NULL);


INSERT INTO public.package_target (package_id, platform, target) VALUES (9003, 'JVM', '1.8');
INSERT INTO public.package_target (package_id, platform, target) VALUES (9003, 'NATIVE', 'linux_x64');


INSERT INTO public.package_target (package_id, platform, target) VALUES (9004, 'JS', NULL);
