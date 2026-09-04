INSERT INTO public.maven_artifact (id, group_id, artifact_id, version)
VALUES (9101, 'com.example', 'non-kmp-artifact', '1.0.0');

INSERT INTO public.non_kmp_packages (id, maven_artifact_id)
VALUES (9101, 9101);

INSERT INTO package_index_request(id, group_id, artifact_id, version, released_ts, scraper_type, reindex, failed_attempts, status)
VALUES (9101, 'com.example', 'non-kmp-artifact', '1.0.0', CURRENT_TIMESTAMP, 'CENTRAL_SONATYPE', false, 0, 'PENDING');
