INSERT INTO public.maven_artifact (id, group_id, artifact_id, version)
VALUES (9201, 'com.example', 'non-kmp-artifact', '1.0.0');

INSERT INTO public.non_kmp_packages (id, maven_artifact_id, release_ts, scraper_type, error_type)
VALUES (9201, 9201, '2026-09-04 00:00:00', 'CENTRAL_SONATYPE', 'MISSING_TOOLING_METADATA');