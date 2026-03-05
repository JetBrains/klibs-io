-- scm_owner (matching data.sql semantics)
INSERT INTO public.scm_owner (
    id, id_native, followers, updated_at,
    login, type, name, description,
    homepage, twitter_handle, email, location, company
) VALUES (
             23, 1446536, 0, CURRENT_TIMESTAMP,
             'Kotlin', 'organization', 'Kotlin', 'Kotlin Tools and Libraries',
             'https://kotlinlang.org', 'kotlin', NULL, NULL, NULL
         );

-- scm_repo (matching data.sql semantics)
INSERT INTO public.scm_repo (
    id_native, id, owner_id,
    has_gh_pages, has_issues, has_wiki, has_readme,
    created_ts, updated_at, last_activity_ts,
    stars, open_issues,
    name, description, homepage,
    license_key, license_name, default_branch
) VALUES (
             99576820, 37, 23,
             false, true, false, true,
             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
             0, 0,
             'kotlinx-atomicfu', 'The idiomatic way to use atomic operations in Kotlin', NULL,
             'other', 'Other', 'master'
         );

-- project
INSERT INTO public.project (
    id, scm_repo_id,
    latest_version_ts, latest_version,
    description,
    name,
    minimized_readme,
    owner_id
) VALUES (
             18, 37,
             CURRENT_TIMESTAMP, '0.26.0',
             'Project description placeholder (not used by these tests)',
             'kotlinx-atomicfu',
             NULL,
             23
         );

-- package: older version (no description)
INSERT INTO public.package (
    id, project_id,
    release_ts, created_at,
    group_id, artifact_id, version,
    description,
    url, scm_url,
    build_tool, build_tool_version, kotlin_version,
    configuration, developers, licenses
) VALUES (
             498, 18,
             CURRENT_TIMESTAMP - INTERVAL '365 days', CURRENT_TIMESTAMP - INTERVAL '365 days',
             'org.jetbrains.kotlinx', 'atomicfu', '0.25.0',
             NULL,
             'https://github.com/Kotlin/kotlinx.atomicfu', 'https://github.com/Kotlin/kotlinx.atomicfu',
             'Gradle', '8.0', '2.0.0',
             '{}'::jsonb, '[]'::jsonb, '[]'::jsonb
         );

-- package: latest version (has description the test asserts)
INSERT INTO public.package (
    id, project_id,
    release_ts, created_at,
    group_id, artifact_id, version,
    description,
    url, scm_url,
    build_tool, build_tool_version, kotlin_version,
    configuration, developers, licenses
) VALUES (
             497, 18,
             CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
             'org.jetbrains.kotlinx', 'atomicfu', '0.26.0',
             'AtomicFU utilities',
             'https://github.com/Kotlin/kotlinx.atomicfu', 'https://github.com/Kotlin/kotlinx.atomicfu',
             'Gradle', '8.7', '2.0.21',
             '{}'::jsonb, '[]'::jsonb, '[]'::jsonb
         );

-- targets for both versions
INSERT INTO public.package_target (package_id, platform, target) VALUES
                                                                     (498, 'JVM', '1.8'),
                                                                     (497, 'JVM', '1.8');