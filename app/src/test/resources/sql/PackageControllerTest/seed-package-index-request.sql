INSERT INTO public.package_index_request (group_id, artifact_id, version, next_attempt_ts, scraper_type)
VALUES ('org.queued', 'libA', '1.0.0', CURRENT_TIMESTAMP, 'SEARCH_MAVEN');

INSERT INTO public.package_index_request (group_id, artifact_id, version, next_attempt_ts, scraper_type)
VALUES ('org.failed', 'libA', '1.0.0', NULL, 'SEARCH_MAVEN');
