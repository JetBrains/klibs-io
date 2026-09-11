package io.klibs.integration.maven.service

import io.klibs.integration.maven.ScraperType
import org.apache.maven.index.Indexer
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Service

@Service
class CentralSonatypeMavenIndexScannerService(
    indexer: Indexer,
    indexingContextManager: MavenIndexingContextManager,
) : MavenIndexScannerService(indexer, indexingContextManager, ScraperType.CENTRAL_SONATYPE)
