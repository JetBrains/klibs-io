-- Seed packages for group removal scenario
-- com.rm:a and com.rm:b should be deleted by removeBannedPackages("com.rm", NULL)
-- com.keep:c should remain

-- com.rm:a
INSERT INTO public.package VALUES (
    8010, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    'com.rm', 'a', '1.0.0',  'A', 'https://ex.com/a',
    'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN'
);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8010, 'JVM', '1.8');

-- com.rm:b
INSERT INTO public.package VALUES (
    8011, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    'com.rm', 'b', '1.0.0', 'B', 'https://ex.com/b',
    'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN'
);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8011, 'JVM', '1.8');

-- com.keep:c
INSERT INTO public.package VALUES (
    8012, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
    'com.keep', 'c', '1.0.0', 'C', 'https://ex.com/c',
    'gradle', '8.0', '2.0.0', '[]'::jsonb, NULL, '[]'::jsonb, '[]'::jsonb, 'SEARCH_MAVEN'
);
INSERT INTO public.package_target (package_id, platform, target) VALUES (8012, 'JVM', '1.8');
