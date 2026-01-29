package io.klibs.integration.maven.service

import io.klibs.integration.maven.MavenIntegrationProperties
import org.apache.maven.index.Indexer
import org.apache.maven.index.context.IndexCreator
import org.apache.maven.index.context.IndexingContext
import org.springframework.stereotype.Service
import java.io.File

@Service
class MavenIndexingContextManager(
    private val properties: MavenIntegrationProperties,
    private val indexer: Indexer,
    private val indexCreators: List<IndexCreator>,
) {
    suspend fun <T> useCentralContext(
        contextId: String,
        block: suspend (IndexingContext) -> T
    ): T {
        val context = createCentralContext(contextId)
        try {
            return block(context)
        } finally {
            indexer.closeIndexingContext(context, false)
        }
    }

    private fun createCentralContext(contextId: String): IndexingContext {
        val centralConfig = properties.central
        val indexDir = File(centralConfig.indexDir)
        if (!indexDir.exists()) {
            indexDir.mkdirs()
        }

        return indexer.createIndexingContext(
            contextId,
            "central",
            indexDir,
            indexDir,
            centralConfig.indexEndpoint,
            null,
            true,
            true,
            indexCreators
        )
    }
}
