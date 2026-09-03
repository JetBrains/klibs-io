-- Reachable repositories on the same owner, so that a single unreachable repo stays a small share of the corpus.
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts,
                             updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key,
                             license_name, default_branch)
SELECT 700000 + n,
       400 + n,
       198,
       false,
       true,
       false,
       true,
       '2023-02-08 01:28:54.000000',
       current_timestamp,
       '2023-02-19 17:44:36.000000',
       0,
       0,
       'k-filler-' || n,
       null, null, null, null,
       'main'
FROM generate_series(1, 7) AS n;
