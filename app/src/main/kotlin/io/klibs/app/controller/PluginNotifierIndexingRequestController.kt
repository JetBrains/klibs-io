package io.klibs.app.controller

import io.klibs.app.api.PluginNotifierIndexingRequest
import io.klibs.app.dto.IndexingRequestValidationResult
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.validator.PluginNotifierRequestValidator
import io.klibs.app.service.PackageIndexingRequestProcessingService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.UriComponentsBuilder

/**
 * Receives notifications from the Gradle klibs-io-notifier plugin about artifacts newly published to Maven Central.
 */
@RestController
@RequestMapping("/notify")
class PluginNotifierIndexingRequestController(
    private val processingService: PackageIndexingRequestProcessingService<PluginNotifierIndexingRequest>,
    private val notifierRequestValidator: PluginNotifierRequestValidator,
) {

    @PostMapping("/artifacts", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun handleRequest(
        @Valid @RequestBody payload: PluginNotifierIndexingRequest
    ): ResponseEntity<out Any> {
        return when (val result = notifierRequestValidator.validatePluginNotifierIndexingRequest(payload)) {
            is IndexingRequestValidationResult.NotApplicable -> result.response
            is IndexingRequestValidationResult.Valid -> {
                try {
                    processingService.processRequest(result.request)
                    val statusUrl = buildStatusURL(result)
                    ResponseEntity.ok()
                        .body("The indexing request has been successfully added to the klibs.io queue. You can check the indexing status of your artifact at $statusUrl")
                } catch (e: UserRequestProcessingException) {
                    ResponseEntity.badRequest()
                        .body("The indexing request could not be processed by klibs.io: ${e.reason}")
                } catch (e: Exception) {
                    logger.error("Notification processing failed with error: $e")
                    ResponseEntity.internalServerError()
                        .body("The indexing request failed due to an internal klibs.io error")
                }
            }
        }
    }

    private fun buildStatusURL(result: IndexingRequestValidationResult.Valid<PluginNotifierIndexingRequest>): String =
        UriComponentsBuilder.fromUriString("https://klibs.io")
            .pathSegment("package", result.request.groupId, result.request.artifactId, result.request.version, "status")
            .build()
            .encode()
            .toUriString()

    private companion object {
        private val logger = LoggerFactory.getLogger(PluginNotifierIndexingRequestController::class.java)
    }
}
