package io.klibs.app.controller

import io.klibs.app.dto.PluginRequestNotification
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Notify", description = "Receives publish notifications from library authors")
class NotifyController {

    @PostMapping("/notify/ping", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun notify(@RequestBody artifacts: List<PluginRequestNotification>): String {
        logger.info("Received ${artifacts.size} artifacts: $artifacts")
        return "Received ${artifacts.size} artifacts"
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(NotifyController::class.java)
    }
}
