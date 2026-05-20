
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (7001, 7001, 0, CURRENT_TIMESTAMP, 'unknown-owner', 'organization', 'Unknown Owner', NULL, NULL, NULL, NULL, NULL, NULL);


INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch)
VALUES (7001, 7001, 7001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 'unknown-repo', 'Repo for unknown target test', NULL, 'apache-2.0', 'Apache License 2.0', 'main');


INSERT INTO public.project VALUES (7001, 7001, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'unknown-repo', NULL, 7001);



-- Auto-seeded maven_artifact rows for package FK (added by test fix)
INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1065883812, 'org.example.unknown', 'unknown-target-lib', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, scraper_type, maven_artifact_id) VALUES (7001, 7001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'org.example.unknown', 'unknown-target-lib', '1.0.0', 'Library with unknown native target', 'https://example.com/unknown', 'https://example.com/unknown', 'gradle', '8.5', '1.9.0', null, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN', 1065883812);


INSERT INTO public.package_target (id, platform, target, package_id)
VALUES (7001, 'NATIVE', 'mystery_os_9000', 7001);


INSERT INTO public.package_target (id, platform, target, package_id)
VALUES (7002, 'JVM', '18', 7001);
