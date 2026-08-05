package io.klibs.core.search.opensearch

import io.klibs.core.search.configuration.properties.OpenSearchProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/** The indices this version builds, one [IndexSpec] each. */
@Component
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class IndexNaming(properties: OpenSearchProperties) {

    val project = IndexSpec(
        base = properties.projectIndex,
        settings = IndexDefinitions.PROJECT_SETTINGS,
        mappings = IndexDefinitions.PROJECT_MAPPINGS,
        sql = IndexDefinitions.PROJECT_DOC_SQL,
    ) { it.get("project_id").asText() }

    val packages = IndexSpec(
        base = properties.packageIndex,
        settings = IndexDefinitions.PACKAGE_SETTINGS,
        mappings = IndexDefinitions.PACKAGE_MAPPINGS,
        sql = IndexDefinitions.PACKAGE_DOC_SQL,
    ) { "${it.get("group_id").asText()}:${it.get("artifact_id").asText()}" }

    val all = listOf(project, packages)
}
