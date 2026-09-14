package io.klibs.app.indexing

import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PackageIndexRequestMetrics(
    private val indexingRequestRepository: IndexingRequestRepository,
    registry: MeterRegistry,
) {

    init {
        Gauge.builder(PACKAGE_INDEX_REQUEST_SIZE) { totalSizeOrNaN() }
            .description("Klibs: Total number of package index requests")
            .register(registry)

        Gauge.builder(PACKAGE_INDEX_REQUEST_FAILED) { failedCountOrNaN() }
            .description("Klibs: Number of package index requests with status FAILED")
            .register(registry)
    }

    private fun totalSizeOrNaN(): Double =
        runCatching { indexingRequestRepository.count().toDouble() }
            .onFailure { logger.debug("Could not count total package index requests", it) }
            .getOrDefault(Double.NaN)

    private fun failedCountOrNaN(): Double =
        runCatching { indexingRequestRepository.countFailed().toDouble() }
            .onFailure { logger.debug("Could not count failed package index requests", it) }
            .getOrDefault(Double.NaN)

    companion object {
        private val logger = LoggerFactory.getLogger(PackageIndexRequestMetrics::class.java)

        const val PACKAGE_INDEX_REQUEST_SIZE = "klibs.package.index.request.size"
        const val PACKAGE_INDEX_REQUEST_FAILED = "klibs.package.index.request.failed"
    }
}
