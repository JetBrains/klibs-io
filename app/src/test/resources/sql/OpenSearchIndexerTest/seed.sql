INSERT INTO public.scm_owner (
    id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company
) VALUES
    (60001, 60001, 0, CURRENT_TIMESTAMP, 'indexer-owner', 'author', 'Indexer Owner', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO public.scm_repo (
    id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts,
    stars, open_issues, name, description, homepage, license_key, license_name, default_branch
) VALUES
    (60001, 60001, 60001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 10, 0, 'repo-i1', 'Repo I1', NULL, 'mit', 'MIT License', 'main'),
    (60002, 60002, 60001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 20, 0, 'repo-i2', 'Repo I2', NULL, 'mit', 'MIT License', 'main'),
    (60003, 60003, 60001, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 30, 0, 'repo-i3', 'Repo I3', NULL, 'mit', 'MIT License', 'main');

INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme, owner_id) VALUES
    (70001, 60001, CURRENT_TIMESTAMP, '1.0.0', 'Project I1', 'repo-i1', NULL, 60001),
    (70002, 60002, CURRENT_TIMESTAMP, '1.0.0', 'Project I2', 'repo-i2', NULL, 60001),
    (70003, 60003, CURRENT_TIMESTAMP, '1.0.0', 'Project I3', 'repo-i3', NULL, 60001);

INSERT INTO public.maven_artifact (id, group_id, artifact_id, version) VALUES
    (1200000001, 'io.indexer', 'lib-i1', '1.0.0'),
    (1200000002, 'io.indexer', 'lib-i2', '1.0.0'),
    (1200000003, 'io.indexer', 'lib-i3', '1.0.0'),
    (1200000004, 'io.indexer', 'lib-i3-extra', '1.0.0')
ON CONFLICT (group_id, artifact_id, version) DO NOTHING;

-- Project 70003 owns two packages, so project docs (3) and package docs (4) differ: a count
-- assertion that passed for the wrong index would show up.
INSERT INTO public.package (id, project_id, release_ts, created_at, group_id, artifact_id, version, description, url, scm_url, build_tool, build_tool_version, kotlin_version, configuration, developers, licenses, maven_artifact_id) VALUES
    (71001, 70001, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.indexer', 'lib-i1', '1.0.0', 'desc I1', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1200000001),
    (71002, 70002, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.indexer', 'lib-i2', '1.0.0', 'desc I2', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1200000002),
    (71003, 70003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.indexer', 'lib-i3', '1.0.0', 'desc I3', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1200000003),
    (71004, 70003, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'io.indexer', 'lib-i3-extra', '1.0.0', 'desc I3 extra', NULL, NULL, 'maven', '3.9.0', '2.0', '{}', '[]', '[{"name":"MIT"}]', 1200000004);

INSERT INTO public.package_target (package_id, platform, target) VALUES
    (71001, 'JVM', NULL),
    (71002, 'JVM', NULL),
    (71003, 'JVM', NULL),
    (71004, 'JVM', NULL);
