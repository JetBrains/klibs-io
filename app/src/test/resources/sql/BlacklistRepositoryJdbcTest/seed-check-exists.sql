-- Seed data: one existing package com.example:lib-a
-- Minimal viable insert following schema used in other tests
INSERT INTO public.package
VALUES (
    8001,               -- id
    NULL,               -- project_id
    CURRENT_TIMESTAMP,  -- release_ts (created_at in some schemas)
    CURRENT_TIMESTAMP,  -- updated_at / release_ts depending on schema
    'com.example',      -- group_id
    'lib-a',            -- artifact_id
    '1.0.0',            -- version
    'com.example:lib-a',-- name
    'Example A',        -- description
    'https://example.com/lib-a', -- url
    'gradle',           -- build_tool
    '8.0',              -- build_tool_version
    '2.0.0',            -- kotlin_version
    '[]'::jsonb,        -- developers
    NULL,               -- configuration
    '[]'::jsonb,        -- licenses
    '[]'::jsonb,        -- extra json column if exists (kept for compatibility)
    'SEARCH_MAVEN'      -- scraper_type
);
