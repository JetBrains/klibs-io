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
        return when(val result = notifierRequestValidator.validatePluginNotifierIndexingRequest(payload)){
            is IndexingRequestValidationResult.NotApplicable -> result.response
            is IndexingRequestValidationResult.Valid -> {
                try {
                    processingService.processRequest(result.request)
                    ResponseEntity.ok().body("Indexing request has been successfully added to the klibs.io queue")
                } catch (e: UserRequestProcessingException) {
                    ResponseEntity.badRequest().body("Indexing request could not be processed by klibs.io: ${e.reason}")
                } catch (e: Exception) {
                    logger.error("Notification processing failed with error: $e")
                    ResponseEntity.internalServerError().body("Indexing request failed due to internal klibs.io error")
                }
            }
        }
    }

    private companion object{
        private val logger = LoggerFactory.getLogger(PluginNotifierIndexingRequestController::class.java)
    }
}
