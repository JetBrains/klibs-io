-- Hides the project seeded by ProjectControllerTest/seed.sql
INSERT INTO public.project_hidden (project_id, hidden_at, origin, reason)
VALUES (18, current_timestamp, 'AUTO', 'GitHub repository unreachable');
