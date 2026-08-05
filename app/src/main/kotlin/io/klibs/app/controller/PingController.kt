package io.klibs.app.controller

import io.klibs.core.search.opensearch.SearchIndexReadiness
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Uptime probe for deployment: `values.base.yaml` points both liveness and readiness at `/ping`.
 * We don't use /actuator/health because our monitoring will not parse the complex output.
 * Returns 503 while the search index is still building, so a fresh pod isn't sent traffic early.
 */
@RestController
class PingController(
    private val searchIndexReadiness: SearchIndexReadiness?,
) {
    @GetMapping("/ping")
    fun ping(): ResponseEntity<String> =
        if (searchIndexReadiness?.isReady() != false) {
            ResponseEntity.ok("pong")
        } else {
            ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("search index not ready")
        }
}
