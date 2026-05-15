package io.klibs.app.auth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.app.configuration.properties.AuthProperties
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import org.springframework.web.util.UriComponentsBuilder

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authProperties: AuthProperties,
    private val jwtService: JwtService,
    private val okHttpClient: OkHttpClient,
    private val objectMapper: ObjectMapper
) {

    @GetMapping("/github/login")
    fun login(): ResponseEntity<Void> {
        val redirectUrl = UriComponentsBuilder
            .fromUriString("https://github.com/login/oauth/authorize")
            .queryParam("client_id", authProperties.github.clientId)
            .build()
            .toUriString()
        return ResponseEntity.status(302).header("Location", redirectUrl).build()
    }

    @GetMapping("/github/callback")
    fun callback(
        @RequestParam code: String,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        logger.debug("GitHub OAuth callback received")
        val accessToken = exchangeCodeForToken(code) ?: run {
            logger.warn("Failed to exchange GitHub OAuth code for access token")
            return ResponseEntity.status(400).build()
        }
        val login = fetchGithubLogin(accessToken) ?: run {
            logger.warn("Failed to fetch GitHub login from access token")
            return ResponseEntity.status(400).build()
        }
        logger.info("GitHub OAuth login successful for: {}", login)

        val cookie = Cookie(JwtAuthFilter.COOKIE_NAME, jwtService.issue(login)).apply {
            isHttpOnly = true
            path = "/"
            maxAge = 3600
            setAttribute("SameSite", "Lax")
        }
        response.addCookie(cookie)
        return ResponseEntity.status(302).header("Location", "/").build()
    }

    @GetMapping("/me")
    fun me(authentication: Authentication?): ResponseEntity<MeResponse> {
        if (authentication == null || authentication is AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                .header("Cache-Control", "no-store")
                .build()
        }
        return ResponseEntity.ok()
            .header("Cache-Control", "no-store")
            .body(MeResponse(authentication.name))
    }

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        val cookie = Cookie(JwtAuthFilter.COOKIE_NAME, "").apply {
            isHttpOnly = true
            path = "/"
            maxAge = 0
            setAttribute("SameSite", "Lax")
        }
        response.addCookie(cookie)
        return ResponseEntity.noContent().build()
    }

    private fun exchangeCodeForToken(code: String): String? {
        val body = FormBody.Builder()
            .add("client_id", authProperties.github.clientId)
            .add("client_secret", authProperties.github.clientSecret)
            .add("code", code)
            .build()
        val request = Request.Builder()
            .url("https://github.com/login/oauth/access_token")
            .post(body)
            .header("Accept", "application/json")
            .build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val bodyStr = resp.body?.string() ?: return null
            val token = objectMapper.readTree(bodyStr).get("access_token")?.asText()
            return if (token.isNullOrBlank()) null else token
        }
    }

    private fun fetchGithubLogin(accessToken: String): String? {
        val request = Request.Builder()
            .url("https://api.github.com/user")
            .header("Authorization", "token $accessToken")
            .header("Accept", "application/vnd.github+json")
            .build()
        okHttpClient.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val bodyStr = resp.body?.string() ?: return null
            val login = objectMapper.readTree(bodyStr).get("login")?.asText()
            return if (login.isNullOrBlank()) null else login
        }
    }

    data class MeResponse(@JsonProperty("login") val login: String)

    companion object {
        private val logger = LoggerFactory.getLogger(AuthController::class.java)
    }
}
