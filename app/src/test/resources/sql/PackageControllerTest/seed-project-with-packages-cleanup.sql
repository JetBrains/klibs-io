-- Remove package targets
DELETE FROM public.package_target WHERE package_id IN (19002, 19003, 19004, 18002);

-- Remove packages
DELETE FROM public.package WHERE id IN (19002, 19003, 19004, 18002);

-- Remove projects
DELETE FROM public.project WHERE id IN (19001, 18001, 19100);

-- Remove scm repositories
DELETE FROM public.scm_repo WHERE id IN (19001, 18001, 19100);

-- Remove scm owner
DELETE FROM public.scm_owner WHERE id = 19001;