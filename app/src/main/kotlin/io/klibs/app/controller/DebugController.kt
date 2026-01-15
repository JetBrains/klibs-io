package io.klibs.app.controller

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Tag(name = "Debug", description = "Internal endpoints to debug something. DO NOT USE FOR ANYTHING!")
@Controller
@ConditionalOnProperty("klibs.debug", havingValue = "true")
@Profile("!prod")
class DebugController(
) {
    @GetMapping("/")
    fun index() = "redirect:/api-docs/swagger-ui.html"
}
