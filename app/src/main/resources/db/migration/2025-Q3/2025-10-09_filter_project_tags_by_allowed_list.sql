CREATE TEMP TABLE filtered_project_tags AS
SELECT DISTINCT pt.project_id, pt.origin, apt.name AS value
FROM project_tags pt
JOIN allowed_project_tags apt
  ON pt.value IS NOT NULL
 AND (pt.value = apt.name OR apt.tag_synonyms @> to_jsonb(ARRAY[pt.value]));

TRUNCATE project_tags;
INSERT INTO project_tags (project_id, origin, value)
SELECT project_id, origin, value
FROM filtered_project_tags;

DROP TABLE filtered_project_tags;
