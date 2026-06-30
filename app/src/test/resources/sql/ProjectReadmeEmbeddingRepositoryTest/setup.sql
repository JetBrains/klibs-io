-- Fixture for ProjectRepositoryJdbc readme-embedding methods.
--   project 9301 — has a minimized_readme but no embedding (must be picked by findWithoutEmbedding).
--   project 9302 — has no minimized_readme (must never be picked).

INSERT INTO scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage,
                       twitter_handle, email, location, company)
VALUES (9301, 9301, 0, CURRENT_TIMESTAMP, 'klibs-embedding-test', 'author',
        'klibs embedding test', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO scm_repo (id, id_native, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                      updated_at, last_activity_ts, stars, open_issues, name, description, homepage,
                      license_key, license_name, default_branch)
VALUES (9301, 9301, 9301, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
        0, 0, 'repo-embed-a', NULL, NULL, 'mit', 'MIT License', 'main'),
       (9302, 9302, 9301, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
        0, 0, 'repo-embed-b', NULL, NULL, 'mit', 'MIT License', 'main');

INSERT INTO project (id, scm_repo_id, owner_id, name, description, minimized_readme,
                     latest_version, latest_version_ts, dependent_count)
VALUES (9301, 9301, 9301, 'project-with-readme', NULL, '# Some README content', '1.0.0', CURRENT_TIMESTAMP, 0),
       (9302, 9302, 9301, 'project-without-readme', NULL, NULL, '1.0.0', CURRENT_TIMESTAMP, 0);
