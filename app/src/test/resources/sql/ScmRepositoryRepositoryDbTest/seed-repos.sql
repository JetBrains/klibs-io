-- One owner + several repos exercising findMultipleForUpdate's eligibility filter.
-- Expected eligible (stale > 24h, no active backoff window), highest-star first: D, A, B, F.
INSERT INTO public.scm_owner (
    id, id_native, followers, updated_at,
    login, type, name, description,
    homepage, twitter_handle, email, location, company
) VALUES (
    800001, 800000001, 0, current_timestamp,
    'retry-test', 'organization', 'Retry Test', NULL,
    NULL, NULL, NULL, NULL, NULL
);

INSERT INTO public.scm_repo (
    id_native, id, owner_id,
    has_gh_pages, has_issues, has_wiki, has_readme,
    created_ts, updated_at, last_activity_ts,
    stars, open_issues,
    name, description, homepage,
    license_key, license_name, default_branch
) VALUES
    -- A: eligible, 50 stars
    (800000010, 800010, 800001, false, true, false, false,
     current_timestamp, current_timestamp - interval '2 days', current_timestamp,
     50, 0, 'repo-a', NULL, NULL, NULL, NULL, 'main'),
    -- B: eligible, 30 stars
    (800000011, 800011, 800001, false, true, false, false,
     current_timestamp, current_timestamp - interval '2 days', current_timestamp,
     30, 0, 'repo-b', NULL, NULL, NULL, NULL, 'main'),
    -- C: excluded — active backoff window (see scm_repo_scheduling below)
    (800000012, 800012, 800001, false, true, false, false,
     current_timestamp, current_timestamp - interval '2 days', current_timestamp,
     40, 0, 'repo-c', NULL, NULL, NULL, NULL, 'main'),
    -- D: eligible, 60 stars — no scheduling row
    (800000013, 800013, 800001, false, true, false, false,
     current_timestamp, current_timestamp - interval '2 days', current_timestamp,
     60, 0, 'repo-d', NULL, NULL, NULL, NULL, 'main'),
    -- E: excluded — updated within the last 24h
    (800000014, 800014, 800001, false, true, false, false,
     current_timestamp, current_timestamp, current_timestamp,
     100, 0, 'repo-e', NULL, NULL, NULL, NULL, 'main'),
    -- F: eligible — backoff window already elapsed (see scm_repo_scheduling below)
    (800000015, 800015, 800001, false, true, false, false,
     current_timestamp, current_timestamp - interval '2 days', current_timestamp,
     20, 0, 'repo-f', NULL, NULL, NULL, NULL, 'main');

-- Scheduling state lives in its own table now; a row exists only while a repo is backed off.
-- C: next retry still in the future -> excluded.  F: window already elapsed -> eligible.
INSERT INTO public.scm_repo_scheduling (
    scm_repo_id, next_retry_at, retry_attempts
) VALUES
    (800012, current_timestamp + interval '1 hour', 2),
    (800015, current_timestamp - interval '1 hour', 3);
