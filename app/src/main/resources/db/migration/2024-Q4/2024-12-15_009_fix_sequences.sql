DO $$
DECLARE
    table_list text[] := ARRAY['package', 'package_index_request', 'project', 'scm_owner', 'scm_repo'];
    t text;
    seq_name text;
    max_id bigint;
BEGIN
    FOREACH t IN ARRAY table_list
        LOOP
            seq_name := t || '_id_seq';
            EXECUTE format('select coalesce(MAX(id), 1) + 100 FROM public.%I', t) INTO max_id;
            perform setval(seq_name, max_id, false);
        END LOOP;
END$$;