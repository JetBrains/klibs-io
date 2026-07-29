package io.klibs.core.search.opensearch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.klibs.core.search.configuration.properties.OpenSearchProperties
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.opensearch.core.BulkRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component

/**
 * TEMPORARY (KTL-4711) full-rebuild loader: projects the project + package docs straight from the
 * base tables (NOT the `project_index` / `package_index` mat views) and bulk-indexes them into
 * OpenSearch. Enough to fill the index for the eval gate; the real incremental sync pipeline
 * (alias-swap, scheduling, JPA read model) is a separate task. Wiped-and-refilled on each call.
 */
@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class OpenSearchTempPopulator(
    private val client: OpenSearchClient,
    private val jdbcClient: JdbcClient,
    private val properties: OpenSearchProperties,
) {

    private val mapper = ObjectMapper()

    fun populateProjects() = rebuild(properties.projectIndex, PROJECT_DOC_SQL, "project_id")

    fun populatePackages() = rebuild(properties.packageIndex, PACKAGE_DOC_SQL) { node ->
        "${node.get("group_id").asText()}:${node.get("artifact_id").asText()}"
    }

    private fun rebuild(index: String, sql: String, idField: String) =
        rebuild(index, sql) { it.get(idField).asText() }

    private fun rebuild(index: String, sql: String, idOf: (ObjectNode) -> String) {
        client.deleteByQuery { d -> d.index(index).query { q -> q.matchAll { it } } }
        val docs = jdbcClient.sql(sql).query(String::class.java).list()
            .map { mapper.readTree(it) as ObjectNode }
        docs.chunked(BATCH).forEach { batch ->
            val bulk = BulkRequest.Builder()
            batch.forEach { node -> bulk.operations { op -> op.index { it.index(index).id(idOf(node)).document(node) } } }
            val response = client.bulk(bulk.build())
            check(!response.errors()) { "bulk into '$index' had errors: ${response.items().firstOrNull { it.error() != null }?.error()?.reason()}" }
        }
        client.indices().refresh { it.index(index) }
        log.info("populated OpenSearch index '{}' with {} docs", index, docs.size)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(OpenSearchTempPopulator::class.java)
        private const val BATCH = 500

        // Project docs from base tables. Mirrors project_index derivation (platforms/targets from
        // package_target, USER>GITHUB>AI tags, GAV agg, nested packages) minus the tsvector.
        private val PROJECT_DOC_SQL = """
            WITH latest_pkg AS (
              SELECT DISTINCT ON (p.group_id, p.artifact_id)
                     p.id AS package_id, p.group_id, p.artifact_id, p.project_id, p.description
              FROM package p
              ORDER BY p.group_id, p.artifact_id, p.release_ts DESC
            ),
            proj_tgt AS (
              SELECT lp.project_id,
                     array_remove(array_agg(DISTINCT pt.platform), NULL) AS platforms,
                     array_remove(array_agg(DISTINCT COALESCE(pt.platform || '_' || pt.target, pt.platform)), NULL) AS targets
              FROM latest_pkg lp
                LEFT JOIN package_target pt ON pt.package_id = lp.package_id
              GROUP BY lp.project_id
            ),
            gav AS (
              SELECT project_id,
                     string_agg(DISTINCT group_id, ' ') AS group_ids,
                     string_agg(DISTINCT artifact_id, ' ') AS artifact_ids,
                     json_agg(json_build_object('group_id', group_id, 'artifact_id', artifact_id,
                                                'latest_description', description)) AS packages
              FROM latest_pkg GROUP BY project_id
            ),
            markers_info AS (
              SELECT project_id, array_agg(DISTINCT type) AS markers FROM project_marker GROUP BY project_id
            ),
            tags_info AS (
              SELECT project_id, COALESCE(
                       array_agg(DISTINCT value ORDER BY value DESC) FILTER (WHERE origin = 'USER'),
                       array_agg(DISTINCT value ORDER BY value DESC) FILTER (WHERE origin = 'GITHUB'),
                       array_agg(DISTINCT value ORDER BY value DESC) FILTER (WHERE origin = 'AI')
                     ) AS tags
              FROM project_tags GROUP BY project_id
            )
            SELECT json_build_object(
              'project_id', project.id,
              'owner_type', owner.type,
              'owner_login', owner.login,
              'repo_name', repo.name,
              'name', project.name,
              'stars', repo.stars,
              'license_name', repo.license_name,
              'latest_version', project.latest_version,
              'latest_version_ts', project.latest_version_ts,
              'platforms', COALESCE(proj_tgt.platforms, ARRAY[]::text[]),
              'targets', COALESCE(proj_tgt.targets, ARRAY[]::text[]),
              'plain_description', COALESCE(project.description, repo.description),
              'project_description', project.description,
              'repo_description', repo.description,
              'tags', COALESCE(tags_info.tags, ARRAY[]::varchar[]),
              'markers', COALESCE(markers_info.markers, ARRAY[]::varchar[]),
              'group_ids', COALESCE(gav.group_ids, ''),
              'artifact_ids', COALESCE(gav.artifact_ids, ''),
              'dependent_count', project.dependent_count,
              'has_readme', (project.minimized_readme IS NOT NULL),
              'packages', COALESCE(gav.packages, '[]'::json)
            )::text
            FROM project
              JOIN scm_owner owner ON project.owner_id = owner.id
              JOIN scm_repo repo ON project.scm_repo_id = repo.id
              JOIN proj_tgt ON proj_tgt.project_id = project.id
              LEFT JOIN gav ON gav.project_id = project.id
              LEFT JOIN markers_info ON markers_info.project_id = project.id
              LEFT JOIN tags_info ON tags_info.project_id = project.id
        """.trimIndent()

        // Package docs from base tables. Mirrors package_index: latest version per (group, artifact),
        // platforms/targets from package_target, minus the tsvector.
        private val PACKAGE_DOC_SQL = """
            WITH latest AS (
              SELECT DISTINCT ON (p.group_id, p.artifact_id)
                     p.group_id, p.artifact_id, p.project_id, p.id AS latest_package_id,
                     p.version AS latest_version, p.description AS latest_description, p.release_ts,
                     (SELECT jsonb_array_elements(p.licenses) ->> 'name' LIMIT 1) AS latest_license_name
              FROM package p
              ORDER BY p.group_id, p.artifact_id, p.release_ts DESC
            ),
            tgt AS (
              SELECT pt.package_id,
                     array_remove(array_agg(DISTINCT pt.platform), NULL) AS platforms,
                     array_remove(array_agg(DISTINCT COALESCE(pt.platform || '_' || pt.target, pt.platform)), NULL) AS targets
              FROM package_target pt GROUP BY pt.package_id
            )
            SELECT json_build_object(
              'group_id', l.group_id,
              'artifact_id', l.artifact_id,
              'project_id', l.project_id,
              'latest_package_id', l.latest_package_id,
              'latest_version', l.latest_version,
              'latest_description', l.latest_description,
              'release_ts', l.release_ts,
              'owner_type', owner.type,
              'owner_login', owner.login,
              'latest_license_name', l.latest_license_name,
              'platforms', COALESCE(tgt.platforms, ARRAY[]::text[]),
              'targets', COALESCE(tgt.targets, ARRAY[]::text[])
            )::text
            FROM latest l
              JOIN project ON l.project_id = project.id
              JOIN scm_repo ON project.scm_repo_id = scm_repo.id
              JOIN scm_owner owner ON scm_repo.owner_id = owner.id
              LEFT JOIN tgt ON tgt.package_id = l.latest_package_id
        """.trimIndent()
    }
}
