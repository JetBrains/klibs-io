UPDATE project
SET tags = tags_with_repo.tags_processed
FROM (
         SELECT pt.tags_processed, repo.id
         FROM project_tags pt
                  JOIN scm_repo repo ON repo.name = pt.project_name
     ) AS tags_with_repo
WHERE project.scm_repo_id = tags_with_repo.id;