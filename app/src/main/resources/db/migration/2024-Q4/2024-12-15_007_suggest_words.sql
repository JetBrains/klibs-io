CREATE MATERIALIZED VIEW suggestion_words AS
SELECT name AS word
FROM scm_repo
UNION
SELECT name
FROM scm_owner;