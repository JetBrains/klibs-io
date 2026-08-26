INSERT INTO public.package_index_request (group_id, artifact_id, version, next_attempt_ts, scraper_type)
VALUES ('org.banned', 'libA', '1.0.0', CURRENT_TIMESTAMP, 'SEARCH_MAVEN');

INSERT INTO public.package_index_request (group_id, artifact_id, version, next_attempt_ts, scraper_type)
VALUES ('org.partially.banned', 'banned', '1.0.0', CURRENT_TIMESTAMP, 'SEARCH_MAVEN');

INSERT INTO public.package_index_request (group_id, artifact_id, version, next_attempt_ts, scraper_type)
VALUES ('org.partially.banned', 'not-banned', '1.0.0', CURRENT_TIMESTAMP, 'SEARCH_MAVEN');

INSERT INTO public.banned_packages (group_id, artifact_id, reason)
VALUES ('org.banned', null, 'banned groupId');

INSERT INTO public.banned_packages (group_id, artifact_id, reason)
VALUES ('org.partially.banned', 'banned', 'banned artifactId');
