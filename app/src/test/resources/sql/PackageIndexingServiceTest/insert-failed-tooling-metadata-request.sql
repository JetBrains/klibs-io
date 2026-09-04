INSERT INTO package_index_request(
    id,
    group_id,
    artifact_id,
    version,
    released_ts,
    scraper_type,
    reindex,
    failed_attempts,
    failed_ts,
    last_error_message,
    status
)
VALUES (
    9201,
    'com.example',
    'stale-tooling-metadata',
    '1.0.0',
    CURRENT_TIMESTAMP,
    'CENTRAL_SONATYPE',
    false,
    999,
    CURRENT_TIMESTAMP,
    'Unable to find tooling metadata for com.example:stale-tooling-metadata:1.0.0(CENTRAL_SONATYPE)',
    'FAILED'
);
