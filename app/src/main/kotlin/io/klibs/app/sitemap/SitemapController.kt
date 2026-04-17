package io.klibs.app.sitemap

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SitemapController(
    private val sitemapService: SitemapService,
) {

    @GetMapping("/sitemap.xml", produces = ["application/xml"])
    fun sitemap(): String = sitemapService.getSitemap()
}
