INSERT INTO package_index_request(id, group_id, artifact_id, version, scraper_type, reindex,
                                  failed_attempts, status, next_attempt_ts)
VALUES (1, 'com.example', 'test-artifact', '1.0.0', 'CENTRAL_SONATYPE', false, 0, 'PENDING', current_timestamp);