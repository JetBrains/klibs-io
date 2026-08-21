-- Minimal fixture: one scm_owner + one project so a candidate row's project_id FK resolves.
-- scm_repo_id is nullable, so the scm_repo chain is skipped; owner_id is NOT NULL (FK to scm_owner).
INSERT INTO scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage,
                       twitter_handle, email, location, company)
VALUES (9001, 9001, 0, CURRENT_TIMESTAMP, 'klibs-test-owner', 'author',
        'klibs test owner', NULL, NULL, NULL, NULL, NULL, NULL);

INSERT INTO project (id, scm_repo_id, owner_id, name, description, minimized_readme,
                     latest_version, latest_version_ts, dependent_count)
VALUES (9001, NULL, 9001, 'test-project', NULL, NULL, '1.0.0', CURRENT_TIMESTAMP, 0);
