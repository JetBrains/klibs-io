package io.klibs.app.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Tag(name = "Notify", description = "Receives publish notifications from library authors")
class NotifyController {

    @PostMapping("/notify", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun notify(@RequestParam params: MultiValueMap<String, String>): String {
        val body = params.entries.joinToString("&") { (key, values) ->
            values.joinToString("&") { value -> "$key=$value" }
        }
        logger.info("Received: $body")
        return "Received: $body"
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(NotifyController::class.java)
    }
}
