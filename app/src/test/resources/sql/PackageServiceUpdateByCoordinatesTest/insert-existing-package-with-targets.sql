
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (8101, 8101, 0, CURRENT_TIMESTAMP, 'test-owner', 'author', 'Test Owner', 'Owner desc', NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (8101, 8101, 8101, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 'sample-repo', 'Repo desc', NULL, 'mit', 'MIT License', 'main');


INSERT INTO public.project VALUES (8101, 8101, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'sample-repo', NULL, 8101);


INSERT INTO public.maven_artifact (id, group_id, artifact_id, version)
VALUES (8201, 'io.klibs', 'sample', '1.0.0');



INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, scraper_type, generated_description, maven_artifact_id) VALUES (8201, 8101, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.klibs', 'sample', '1.0.0', 'Old desc', 'https://example.com/sample', NULL, 'gradle', '8.0', '2.0.0', NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN', false, 8201);



INSERT INTO public.package_target VALUES (8201, 'JVM', '1.8', 90001);
INSERT INTO public.package_target VALUES (8201, 'JS', 'ir', 90002);
