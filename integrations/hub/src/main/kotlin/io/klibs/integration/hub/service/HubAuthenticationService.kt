package io.klibs.integration.hub.service

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.integration.hub.configuration.HUB_AUTHENTICATION_ENABLED_PROPERTY
import io.klibs.integration.hub.configuration.HubClientSettings
import io.klibs.integration.hub.enums.HubAuthenticationFailure
import io.klibs.integration.hub.exception.HubAuthenticationException
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import java.io.IOException
import java.io.InterruptedIOException
import java.net.URI
import java.util.concurrent.TimeUnit

@Service
@ConditionalOnProperty(HUB_AUTHENTICATION_ENABLED_PROPERTY, havingValue = "true")
class HubAuthenticationService(
    private val hubHttpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    private val settings: HubClientSettings,
) {
    private val baseUrl = settings.baseUrl.trimEnd('/')

    fun createAuthorizationUri(state: String, codeChallenge: String): URI =
        baseUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/rest/oauth2/auth")
            .addQueryParameter("response_type", "code")
            .addQueryParameter("client_id", settings.clientId)
            .addQueryParameter("redirect_uri", settings.callbackUrl)
            .addQueryParameter("scope", settings.scope)
            .addQueryParameter("state", state)
            .addQueryParameter("request_credentials", "default")
            .addQueryParameter("code_challenge", codeChallenge)
            .addQueryParameter("code_challenge_method", "S256")
            .build()
            .toUri()

    fun authenticate(code: String, codeVerifier: String): String {
        val deadline = System.nanoTime() + settings.callbackTimeout.toNanos()
        try {
            val tokenResponse = execute(tokenRequest(code, codeVerifier), deadline, tokenExchange = true)
            val token = requiredText(tokenResponse, "access_token")
            if (!requiredText(tokenResponse, "token_type").equals("Bearer", ignoreCase = true)) {
                throw HubAuthenticationException(HubAuthenticationFailure.MALFORMED_RESPONSE)
            }
            val user = execute(userInfoRequest(token), deadline, tokenExchange = false)
            return requiredText(user, "sub")
        } catch (exception: Exception) {
            val failure = when (exception) {
                is HubAuthenticationException -> exception.failure
                is InterruptedIOException -> HubAuthenticationFailure.TIMEOUT
                is JsonProcessingException, is IllegalArgumentException -> HubAuthenticationFailure.MALFORMED_RESPONSE
                is IOException -> HubAuthenticationFailure.UNAVAILABLE
                else -> throw exception
            }
            log.warn("Hub authentication failed: {}", failure)
            if (exception is HubAuthenticationException) throw exception
            throw HubAuthenticationException(failure)
        }
    }

    // klibs.io is a public Hub client: it sends client_id instead of a secret, and PKCE proves the code is ours.
    private fun tokenRequest(code: String, codeVerifier: String): Request = Request.Builder()
        .url("$baseUrl/api/rest/oauth2/token")
        .header("Accept", "application/json")
        .post(
            FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", settings.callbackUrl)
                .add("client_id", settings.clientId)
                .add("code_verifier", codeVerifier)
                .build(),
        )
        .build()

    private fun userInfoRequest(token: String): Request = Request.Builder()
        .url("$baseUrl/api/rest/oauth2/userinfo")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer $token")
        .build()

    private fun execute(request: Request, deadline: Long, tokenExchange: Boolean): JsonNode {
        val call = hubHttpClient.newCall(request)
        // The second call gets only the budget left by the first, including reading its response body.
        call.timeout().timeout(minOf(settings.requestTimeout.toNanos(), remainingNanos(deadline)), TimeUnit.NANOSECONDS)
        return call.execute().use { response ->
            parseResponse(response, tokenExchange)
        }
    }

    private fun parseResponse(response: Response, tokenExchange: Boolean): JsonNode {
        if (!response.isSuccessful && (!tokenExchange || response.code !in OAUTH_ERROR_STATUSES)) {
            throw HubAuthenticationException(HubAuthenticationFailure.UNAVAILABLE)
        }
        val json = objectMapper.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .readTree(response.body!!.byteStream())
        if (tokenExchange && response.code in OAUTH_ERROR_STATUSES && json["error"]?.isTextual == true) {
            throw HubAuthenticationException(HubAuthenticationFailure.OAUTH_ERROR)
        }
        if (!response.isSuccessful || json.has("error")) {
            throw HubAuthenticationException(HubAuthenticationFailure.MALFORMED_RESPONSE)
        }
        return json
    }

    private fun requiredText(json: JsonNode, field: String): String =
        json[field]?.takeIf { it.isTextual }?.textValue()?.takeIf { it.isNotBlank() }
            ?: throw HubAuthenticationException(HubAuthenticationFailure.MALFORMED_RESPONSE)

    private fun remainingNanos(deadline: Long): Long = (deadline - System.nanoTime()).also {
        if (it <= 0) throw HubAuthenticationException(HubAuthenticationFailure.TIMEOUT)
    }

    private companion object {
        val log = LoggerFactory.getLogger(HubAuthenticationService::class.java)
        val OAUTH_ERROR_STATUSES = 400..401
    }
}
