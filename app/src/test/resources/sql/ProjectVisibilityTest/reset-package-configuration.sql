-- ProjectControllerTest/seed.sql stores configuration as '{}', which does not deserialize into Configuration.
-- Package details are not what that seed is about; this makes the row readable through /package/../details.
UPDATE public.package SET configuration = NULL WHERE project_id = 18;
