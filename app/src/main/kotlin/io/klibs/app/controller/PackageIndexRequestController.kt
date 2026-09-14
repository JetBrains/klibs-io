package io.klibs.app.controller

import io.klibs.app.api.ProcessPackageIndexRequestsRequest
import io.klibs.app.api.ProcessPackageIndexRequestsResponse
import io.klibs.app.indexing.ManualPackageIndexProcessingService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/package-index-request")
@Tag(name = "Package Index Requests", description = "Operations for managing package indexing requests")
class PackageIndexRequestController(
    private val manualPackageIndexProcessingService: ManualPackageIndexProcessingService,
) {

    @Operation(
        summary = "Manually process package index requests",
        description = "Processes package index requests by removing them or marking the package as non-KMP",
    )
    @PostMapping(path = [ "/manual/process"])
    fun process(
        @RequestBody request: ProcessPackageIndexRequestsRequest,
    ): ResponseEntity<ProcessPackageIndexRequestsResponse> {
        val response = manualPackageIndexProcessingService.process(request.toDTO())
        return ResponseEntity.ok(response)
    }
}
