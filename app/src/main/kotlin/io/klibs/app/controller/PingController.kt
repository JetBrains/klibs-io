package io.klibs.app.controller

import io.klibs.app.search.SearchIndexReadiness
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

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
