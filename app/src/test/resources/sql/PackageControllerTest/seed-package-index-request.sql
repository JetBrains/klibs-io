INSERT INTO public.package_index_request (group_id, artifact_id, version, next_attempt_ts, scraper_type, status)
VALUES ('org.queued', 'libA', '1.0.0', CURRENT_TIMESTAMP, 'SEARCH_MAVEN', 'PENDING');

INSERT INTO public.package_index_request (group_id, artifact_id, version, next_attempt_ts, scraper_type, status)
VALUES ('org.failed', 'libA', '1.0.0', 'infinity', 'SEARCH_MAVEN', 'FAILED');
