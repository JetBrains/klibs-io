-- Insert test scm_owner
INSERT INTO public.scm_owner (id, id_native, followers, updated_at, login, type, name, description, homepage, twitter_handle, email, location, company)
VALUES (8101, 8101, 0, CURRENT_TIMESTAMP, 'test-owner', 'author', 'Test Owner', 'Owner desc', NULL, NULL, NULL, NULL, NULL);

-- Insert test scm_repo
INSERT INTO public.scm_repo (id_native, id, owner_id, has_gh_pages, has_issues, has_wiki, has_readme, created_ts, updated_at, last_activity_ts, stars, open_issues, name, description, homepage, license_key, license_name, default_branch, minimized_readme)
VALUES (8101, 8101, 8101, false, true, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0, 'sample-repo', 'Repo desc', NULL, 'mit', 'MIT License', 'main', 'README');

-- Insert test project
INSERT INTO public.project VALUES (8101, 8101, CURRENT_TIMESTAMP, '1.0.0', CURRENT_TIMESTAMP, 'sample-repo', NULL, 8101);

-- Insert existing package (matches entity fields used by repository and service)
-- Columns order (from existing fixtures): id, project_id, created_at, release_ts, group_id, artifact_id, version,
-- name, description, url, build_tool, build_tool_version, kotlin_version, developers, configuration, licenses, scm_url, scraper_type
INSERT INTO public.package VALUES (
    8201,               -- id
    8101,               -- project_id
    CURRENT_TIMESTAMP,  -- created_at
    CURRENT_TIMESTAMP,  -- release_ts
    'io.klibs',         -- group_id
    'sample',           -- artifact_id
    '1.0.0',            -- version
    'io.klibs:sample',  -- name
    'Old desc',         -- description
    'https://example.com/sample', -- url
    'gradle',           -- build_tool
    '8.0',              -- build_tool_version
    '2.0.0',            -- kotlin_version
    '[]'::jsonb,        -- developers
    null,               -- configuration
    '[]'::jsonb,        -- licenses
    '[]'::jsonb,        -- (kept to follow the same column order as other tests)
    'SEARCH_MAVEN'      -- scraper_type
);

-- Insert two targets for the package: JVM:1.8 and JS:ir
-- Columns order (from data.sql): package_id, platform, target, id
INSERT INTO public.package_target VALUES (8201, 'JVM', '1.8', 90001);
INSERT INTO public.package_target VALUES (8201, 'JS', 'ir', 90002);
