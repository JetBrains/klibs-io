package io.klibs.app.service.impl

import io.klibs.app.api.PluginNotifierIndexingRequest
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.service.PackageIndexingRequestProcessingService
import io.klibs.app.service.UserIndexingRequestService
import io.klibs.integration.maven.dto.GavCoordinatesDTO
import io.klibs.integration.maven.utils.MavenCoordinateDTOUtils
import org.springframework.stereotype.Service

@Service
class PluginNotifierRequestService(
    private val userIndexingRequestService: UserIndexingRequestService,
) : PackageIndexingRequestProcessingService<PluginNotifierIndexingRequest> {

    override fun processRequest(request: PluginNotifierIndexingRequest) {
        validateGAV(request)
        userIndexingRequestService.saveGAVRequest(
            request.groupId,
            request.artifactId,
            request.version,
        )
    }

    private fun validateGAV(request: PluginNotifierIndexingRequest) {
        MavenCoordinateDTOUtils.validateGAVField(
            GavCoordinatesDTO(
                request.groupId,
                request.artifactId,
                request.version
            )
        )?.let { throw UserRequestProcessingException(it) }
    }
}
