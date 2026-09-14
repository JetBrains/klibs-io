package io.klibs.app.indexing

import io.klibs.app.api.ProcessPackageIndexRequestsResponse
import io.klibs.app.dto.ProcessPackageIndexRequestsDTO
import io.klibs.app.dto.ProcessPackageIndexResultDTO
import io.klibs.app.enums.ProcessResultStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ManualPackageIndexProcessingService(
    private val processor: ManualPackageIndexProcessor,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(ManualPackageIndexProcessingService::class.java)
    }

    fun process(request: ProcessPackageIndexRequestsDTO): ProcessPackageIndexRequestsResponse {
        val results = request.packages.map { pkg ->
            try {
                processor.process(request, pkg).also { result ->
                    if (result.status == ProcessResultStatus.SUCCESS) {
                        logger.info(
                            "Committed action {} for {}:{}:{}; affected {} queue request(s)",
                            request.action, pkg.groupId, pkg.artifactId, pkg.version, result.affectedRequestsCount,
                        )
                    }
                }
            } catch (e: Exception) {
                logger.error(
                    "Failed to process coordinate {}:{}:{} with action {}",
                    pkg.groupId, pkg.artifactId, pkg.version, request.action, e,
                )
                ProcessPackageIndexResultDTO(
                    groupId = pkg.groupId,
                    artifactId = pkg.artifactId,
                    version = pkg.version,
                    status = ProcessResultStatus.ERROR,
                    action = request.action,
                    message = e.message ?: "Unexpected error occurred",
                    affectedRequestsCount = 0,
                )
            }
        }
        return ProcessPackageIndexRequestsResponse(totalProcessed = results.size, results = results)
    }
}
