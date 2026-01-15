-- Seed data for banned_packages: one exact ban and one group-wide ban
INSERT INTO public.banned_packages (group_id, artifact_id, reason)
VALUES
  ('com.ban', 'exact-art', 'test'),
  ('com.ban.all', NULL, 'test');
