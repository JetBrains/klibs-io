-- Fixture for similarity search by README embedding.
--   projects 60001/60002/60003 — get distinct embeddings assigned in the test.
--   project 60004 — never gets an embedding (must be excluded from results).

INSERT INTO public.scm_owner (
    id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company
) VALUES
    (60000, 60000, 0, CURRENT_TIMESTAMP, 'similar-owner', 'author', 'Similar Owner', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.scm_repo (
    id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts,
    stars, open_issues, name, description, homepage, license_key, license_name, default_branch
) VALUES
    (60001, 60001, 60000, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 0, 'repo-s1', 'Repo S1', NULL, 'mit', 'MIT License', 'main'),
    (60002, 60002, 60000, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 0, 'repo-s2', 'Repo S2', NULL, 'mit', 'MIT License', 'main'),
    (60003, 60003, 60000, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 0, 'repo-s3', 'Repo S3', NULL, 'mit', 'MIT License', 'main'),
    (60004, 60004, 60000, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 0, 'repo-s4', 'Repo S4', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, dependent_count, owner_id) VALUES
    (60001, 60001, CURRENT_TIMESTAMP, '1.0.0', 'Project S1', 'repo-s1', '# S1', 0, 60000),
    (60002, 60002, CURRENT_TIMESTAMP, '1.0.0', 'Project S2', 'repo-s2', '# S2', 0, 60000),
    (60003, 60003, CURRENT_TIMESTAMP, '1.0.0', 'Project S3', 'repo-s3', '# S3', 0, 60000),
    (60004, 60004, CURRENT_TIMESTAMP, '1.0.0', 'Project S4', 'repo-s4', '# S4', 0, 60000);

INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1160000001, 'io.similar', 'lib-s1', '1.0.0'),
    (1160000002, 'io.similar', 'lib-s2', '1.0.0'),
    (1160000003, 'io.similar', 'lib-s3', '1.0.0'),
    (1160000004, 'io.similar', 'lib-s4', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, maven_artifact_id) VALUES
    (61001, 60001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.similar', 'lib-s1', '1.0.0', 'desc S1', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1160000001),
    (61002, 60002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.similar', 'lib-s2', '1.0.0', 'desc S2', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1160000002),
    (61003, 60003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.similar', 'lib-s3', '1.0.0', 'desc S3', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1160000003),
    (61004, 60004, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.similar', 'lib-s4', '1.0.0', 'desc S4', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1160000004);

INSERT INTO public.package_target (package_id, platform, target) VALUES
    (61001, 'JVM', NULL),
    (61002, 'JVM', NULL),
    (61003, 'JVM', NULL),
    (61004, 'JVM', NULL);
