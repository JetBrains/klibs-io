-- Seed packages for exact artifact removal scenario
-- com.one:keep should remain; com.one:del should be removed

-- com.one:keep
INSERT INTO public.package VALUES (
    8020, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    'com.one', 'keep', '1.0.0', 'com.one:keep', 'Keep', 'https://ex.com/keep',
    'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN'
);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8020, 'JVM', '1.8');

-- com.one:del
INSERT INTO public.package VALUES (
    8021, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    'com.one', 'del', '1.0.0', 'com.one:del', 'Del', 'https://ex.com/del',
    'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN'
);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8021, 'JVM', '1.8');
