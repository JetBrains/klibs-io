-- Truncate all tables in the current schema except selected ones
-- Currently excluded: databasechangelog (Liquibase changelog table)
CREATE OR REPLACE FUNCTION truncate_all_tables()
    RETURNS void AS '
    DECLARE
        tables TEXT;
    BEGIN
        SELECT INTO tables string_agg(
            format(''%I.%I'', schemaname, tablename),
            '', ''
            ORDER BY schemaname, tablename
        )
        FROM pg_tables
        WHERE schemaname = current_schema()
          AND tablename NOT IN (''databasechangelog'', ''maven_central_log'', ''category'', ''allowed_project_tags'');

        IF tables IS NOT NULL THEN
            -- Wait up to 5s for conflicting locks instead of failing instantly with a deadlock.
            EXECUTE ''SET LOCAL lock_timeout = ''''5s'''''';
            EXECUTE ''TRUNCATE TABLE '' || tables || '' CASCADE'';
        END IF;

        UPDATE maven_central_log SET
            maven_index_timestamp = current_date - interval ''10 years''
        WHERE id = 1;
    END;
' LANGUAGE plpgsql;

SELECT truncate_all_tables();
