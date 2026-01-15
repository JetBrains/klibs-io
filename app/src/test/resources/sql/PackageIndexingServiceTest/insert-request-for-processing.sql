INSERT INTO package_index_request(id, group_id, artifact_id, version, released_ts, scraper_type, reindex,
                                  failed_attempts)
VALUES (1, 'com.example', 'test-artifact', '1.0.0', current_timestamp, 'CENTRAL_SONATYPE', false, 0);