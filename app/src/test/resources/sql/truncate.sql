-- Truncate all tables in the current schema except selected ones
-- Currently excluded: databasechangelog (Liquibase changelog table)
CREATE OR REPLACE FUNCTION truncate_all_tables()
    RETURNS void AS '
    DECLARE
        stmt TEXT;
    BEGIN
        SELECT INTO stmt string_agg(
            format(''TRUNCATE TABLE %I.%I CASCADE;'', schemaname, tablename),
            '' ''
        )
        FROM pg_tables
        WHERE schemaname = current_schema()
          AND tablename NOT IN (''databasechangelog'', ''maven_central_log'');

        IF stmt IS NOT NULL THEN
            EXECUTE stmt;
        END IF;
    END;
' LANGUAGE plpgsql;

SELECT truncate_all_tables();
