package io.klibs.integration.maven.service

import io.klibs.integration.maven.ScraperType
import org.apache.maven.index.Indexer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty("klibs.indexing-configuration.central-sonatype.enabled", havingValue = "true")
class CentralSonatypeMavenIndexScannerService(
    indexer: Indexer,
    indexingContextManager: MavenIndexingContextManager,
) : MavenIndexScannerService(indexer, indexingContextManager, ScraperType.CENTRAL_SONATYPE)
