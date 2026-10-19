package io.klibs.app.controller

import io.klibs.app.api.BanSuspiciousCandidateRequest
import io.klibs.app.api.KeepSuspiciousCandidateRequest
import io.klibs.app.api.SuspiciousCandidateResponse
import io.klibs.app.service.SuspiciousCandidateReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("/suspicious-candidates")
@Tag(name = "Suspicious Candidates", description = "Review of artifacts published under more than one groupId")
class SuspiciousCandidateController(
    private val suspiciousCandidateReviewService: SuspiciousCandidateReviewService,
) {

    @Operation(
        summary = "List PENDING candidates",
        description = "Ordered by (projectId, groupId, artifactId). A page shorter than limit is the last one.",
    )
    @GetMapping
    fun findPending(
        @RequestParam("page", required = false, defaultValue = "1")
        @Parameter(
            description = "Page index beginning with 1 (1..N)",
            schema = Schema(type = "integer", minimum = "1", defaultValue = "1")
        )
        @Min(value = 1, message = "Page must be >= 1")
        page: Int,

        @RequestParam("limit", required = false, defaultValue = "100")
        @Parameter(
            description = "The size of the page to be returned",
            schema = Schema(type = "integer", minimum = "1", maximum = "1000", defaultValue = "100")
        )
        @Min(value = 1, message = "Limit must be >= 1")
        @Max(value = 1000, message = "Limit must be <= 1000")
        limit: Int,
    ): List<SuspiciousCandidateResponse> = suspiciousCandidateReviewService.findPending(page, limit)

    @Operation(
        summary = "Keep a groupId",
        description = "Resolves every PENDING row of (projectId, groupId). 409 when there are none.",
    )
    @PostMapping("/keep")
    fun keep(@Valid @RequestBody request: KeepSuspiciousCandidateRequest): ResponseEntity<String> =
        if (suspiciousCandidateReviewService.keep(request)) {
            ResponseEntity.ok("Resolved")
        } else {
            ResponseEntity.status(HttpStatus.CONFLICT).body("No PENDING rows for this groupId in the project")
        }

    @Operation(
        summary = "Ban one artifact",
        description = "Resolves the PENDING row and bans groupId:artifactId globally, in one transaction. " +
            "409 when the row is not PENDING, or the package is missing or already banned.",
    )
    @PostMapping("/ban")
    fun ban(@Valid @RequestBody request: BanSuspiciousCandidateRequest): ResponseEntity<String> {
        suspiciousCandidateReviewService.banCandidate(
            request.projectId, request.artifactId, request.groupId, request.note
        )
        return ResponseEntity.ok("Resolved and banned")
    }
}
