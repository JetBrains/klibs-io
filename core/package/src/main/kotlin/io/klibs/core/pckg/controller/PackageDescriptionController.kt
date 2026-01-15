package io.klibs.core.pckg.controller

import io.klibs.core.pckg.api.UpdateDescriptionRequest
import io.klibs.core.pckg.service.PackageDescriptionService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/package-description")
@Tag(name = "Package Descriptions", description = "Operations related to package descriptions")
class PackageDescriptionController(
    private val packageDescriptionService: PackageDescriptionService
) {
    private val logger = LoggerFactory.getLogger(PackageDescriptionController::class.java)
    @Operation(
        summary = "Generate a description for a specific package and update it",
        description = "Generates a description for a package identified by groupId, and optionally artifactId and version using AI (and updates the package)"
    )
    @GetMapping(path = ["/{groupId}", "/{groupId}/{artifactId}", "/{groupId}/{artifactId}/{version}"])
    fun generateDescription(
        @PathVariable groupId: String,
        @PathVariable(required = false) artifactId: String?,
        @PathVariable(required = false) version: String?
    ): String {
        return packageDescriptionService.generateDescription(groupId, artifactId, version)
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Operation(
        summary = "Generate unique descriptions for packages with duplicate descriptions and update them in the database",
        description = "Finds all packages with duplicate descriptions, generates new unique descriptions for them using AI, and updates the descriptions in the database"
    )
    @PostMapping("/generate-unique")
    fun generateUniqueDescriptions(): String {
        logger.info("Starting unique descriptions generation in a separate thread with 24-hour timeout")

        GlobalScope.launch(Dispatchers.IO) {
            try {
                logger.info("Executing unique descriptions generation")
                val timeoutMillis = TimeUnit.HOURS.toMillis(24) // 24 hours in milliseconds

                val result = withTimeoutOrNull(timeoutMillis) {
                    packageDescriptionService.generateUniqueDescriptions()
                    true
                }

                if (result == true) {
                    logger.info("Unique descriptions generation completed successfully")
                } else {
                    logger.error("Unique descriptions generation timed out after 24 hours")
                }
            } catch (e: Exception) {
                logger.error("Error during unique descriptions generation", e)
            }
        }

        return "Unique descriptions generation started successfully"
    }


    @Operation(
        summary = "Update a package description directly",
        description = "Updates the description of a package identified by groupId, artifactId, and version with a user-provided description"
    )
    @PostMapping("/{groupId}/{artifactId}/{version}")
    fun updateDescription(
        @PathVariable groupId: String,
        @PathVariable artifactId: String,
        @PathVariable version: String,
        @RequestBody request: UpdateDescriptionRequest
    ): String {
        return packageDescriptionService.updatePackageDescription(groupId, artifactId, version, request.description)
    }
}
