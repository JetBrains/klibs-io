package io.klibs.core.project.blacklist

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/blacklist")
@Tag(name = "Blacklist", description = "Operations for managing blacklisted packages")
class BlacklistController(
    private val blacklistService: BlacklistService
) {
    @Operation(summary = "Ban a package by adding it to the banned_packages table and removing it from the packages table")
    @PostMapping("/add")
    fun banPackage(
        @RequestParam(name = "groupId")
        @Parameter(
            description = "Group ID of the Maven artifact",
            example = "org.danbrough.ktor"
        )
        groupId: String,

        @RequestParam(name = "artifactId", required = false)
        @Parameter(
            description = "Artifact ID of the Maven artifact",
            example = "ktor-client"
        )
        artifactId: String?,

        @RequestParam(name = "reason", required = false)
        @Parameter(
            description = "Reason for the blacklisting",
            example = "Malicious package"
        )
        reason: String?

    ): ResponseEntity<String> {
        val actualReason = reason?.takeIf { it.isNotBlank() }
        val success = if (artifactId != null) {
            blacklistService.banPackage(groupId, artifactId, actualReason)
        } else {
            blacklistService.banByGroup(groupId, actualReason)
        }

        return if (success) {
            ResponseEntity.ok("Package $groupId:$artifactId has been banned successfully")
        } else {
            ResponseEntity.badRequest().body("Failed to ban package $groupId:$artifactId. It might not exist or is already banned.")
        }
    }
}
