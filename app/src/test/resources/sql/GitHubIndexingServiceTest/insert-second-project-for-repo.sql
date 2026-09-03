-- A second project backed by the same repository, to cover the multi-project fan-out of hiding
INSERT INTO public.project (id, scm_repo_id, latest_version_ts, latest_version, description, name, minimized_readme,
                            owner_id)
VALUES (10002,
        368,
        current_timestamp,
        '1.0.0',
        'Second project on the same repository',
        'k-big-numbers-extras',
        'initial minimized readme',
        198);
