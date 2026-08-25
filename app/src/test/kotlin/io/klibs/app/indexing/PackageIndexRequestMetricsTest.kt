package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.app.indexing.PackageIndexRequestMetrics.Companion.PACKAGE_INDEX_REQUEST_FAILED
import io.klibs.app.indexing.PackageIndexRequestMetrics.Companion.PACKAGE_INDEX_REQUEST_SIZE
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.enums.IndexingRequestStatus
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.integration.maven.ScraperType
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PackageIndexRequestMetricsTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @Autowired
    private lateinit var indexingRequestRepository: IndexingRequestRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `should register and update package index request metrics`() {
        val sizeGauge = assertNotNull(meterRegistry.find(PACKAGE_INDEX_REQUEST_SIZE).gauge())
        val failedGauge = assertNotNull(meterRegistry.find(PACKAGE_INDEX_REQUEST_FAILED).gauge())

        assertEquals(0.0, sizeGauge.value())
        assertEquals(0.0, failedGauge.value())

        indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.example",
                artifactId = "artifact-one",
                version = "1.0.0",
                repo = ScraperType.SEARCH_MAVEN,
                status = IndexingRequestStatus.PENDING,
            )
        )

        val failed = indexingRequestRepository.save(
            IndexingRequestEntity(
                groupId = "org.example",
                artifactId = "artifact-two",
                version = "1.0.0",
                repo = ScraperType.SEARCH_MAVEN,
                status = IndexingRequestStatus.PENDING,
            )
        )
        jdbcTemplate.update("UPDATE package_index_request SET status = 'FAILED' WHERE id = ?", failed.id)

        assertEquals(2.0, sizeGauge.value())
        assertEquals(1.0, failedGauge.value())
    }

    @Test
    fun `should return NaN when repository throws exception`() {
        val failingRepo = org.mockito.kotlin.mock<IndexingRequestRepository> {
            org.mockito.kotlin.whenever(it.count()).thenThrow(RuntimeException("DB error"))
            org.mockito.kotlin.whenever(it.countByStatus(IndexingRequestStatus.FAILED)).thenThrow(RuntimeException("DB error"))
        }
        val registry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        PackageIndexRequestMetrics(failingRepo, registry)

        val sizeGauge = registry.get(PACKAGE_INDEX_REQUEST_SIZE).gauge()
        val failedGauge = registry.get(PACKAGE_INDEX_REQUEST_FAILED).gauge()

        kotlin.test.assertTrue(sizeGauge.value().isNaN())
        kotlin.test.assertTrue(failedGauge.value().isNaN())
    }
}
