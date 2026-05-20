
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (19001, 19001, 0, CURRENT_TIMESTAMP, 'owner-9001', 'author', 'Owner 9001', 'Owner desc', NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (19001, 19001, 19001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-9001', 'Repo 9001', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (18001, 18001, 19001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-8001', 'Repo 8001', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (19100, 19100, 19001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 1, 'repo-9100', 'Repo 9100', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project VALUES (19001, 19001, CURRENT_TIMESTAMP, '2.0.0', CURRENT_TIMESTAMP, 'repo-9001', 'readme', 19001);


INSERT INTO public.project VALUES (18001, 18001, CURRENT_TIMESTAMP, '2.0.0', CURRENT_TIMESTAMP, 'repo-8001', 'readme', 19001);


INSERT INTO public.project VALUES (19100, 19100, CURRENT_TIMESTAMP, '0.0.0', CURRENT_TIMESTAMP, 'repo-9100', 'readme', 19001);




INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1061382813, 'org.example', 'libA', '1.0.0'),
    (1019966004, 'org.example', 'libA', '2.0.0'),
    (1038860513, 'org.example', 'libB', '3.1.4'),
    (1047858273, 'org.second', 'libC', '2.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (19002, 19001, CURRENT_TIMESTAMP - INTERVAL '1 year', CURRENT_TIMESTAMP - INTERVAL '1 year', 'org.example', 'libA', '1.0.0', 'Old A', 'https://example.com/libA', 'gradle', '7.0', '1.9.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1061382813);


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (19003, 19001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'libA', '2.0.0', 'New A', 'https://example.com/libA', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1019966004);


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (19004, 19001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example', 'libB', '3.1.4', 'Lib B', 'https://example.com/libB', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1038860513);


INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scraper_type, maven_artifact_id) VALUES (18002, 18001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.second', 'libC', '2.0.0', 'Lib C', 'https://second.com/libC', 'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, 'SEARCH_MAVEN', 1047858273);


INSERT INTO public.package_target (package_id, platform, target) VALUES (19002, 'NATIVE', 'linux_x64');


INSERT INTO public.package_target (package_id, platform, target) VALUES (19003, 'JVM', '1.8');
INSERT INTO public.package_target (package_id, platform, target) VALUES (19003, 'JS', NULL);


INSERT INTO public.package_target (package_id, platform, target) VALUES (19004, 'JS', NULL);


INSERT INTO public.package_target (package_id, platform, target) VALUES (18002, 'JS', NULL);