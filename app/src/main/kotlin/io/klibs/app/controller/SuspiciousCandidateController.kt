package io.klibs.app.controller

import io.klibs.app.api.BanSuspiciousCandidateRequest
import io.klibs.app.api.KeepSuspiciousCandidateRequest
import io.klibs.app.api.SuspiciousCandidateResponse
import io.klibs.app.service.SuspiciousCandidateReviewService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/suspicious-candidates")
@Tag(name = "Suspicious Candidates", description = "Review of artifacts published under more than one groupId")
class SuspiciousCandidateController(
    private val suspiciousCandidateReviewService: SuspiciousCandidateReviewService,
) {

    @Operation(summary = "List PENDING candidates")
    @GetMapping
    fun findPending(): List<SuspiciousCandidateResponse> = suspiciousCandidateReviewService.findPending()

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
