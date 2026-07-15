package io.klibs.integration.github

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.authorization.AuthorizationProvider
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class GetCommitAuthorCountsTest {

    @Test
    fun `uses selected authorization provider for GraphQL request`() {
        var capturedRequest: Request? = null
        val interceptor = Interceptor { chain ->
            capturedRequest = chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(GRAPHQL_RESPONSE.toResponseBody())
                .build()
        }
        val integration = GitHubIntegrationKohsukeLibrary(
            SimpleMeterRegistry(),
            GitHubBuilder().build(),
            OkHttpClient.Builder().addInterceptor(interceptor).build(),
            AuthorizationProvider { AUTHORIZATION },
            jacksonObjectMapper(),
            "JetBrains/klibs-io",
        )

        val result = integration.getCommitAuthorCounts("JetBrains", "klibs-io", Instant.EPOCH)

        assertEquals(emptyMap(), result)
        assertEquals(AUTHORIZATION, requireNotNull(capturedRequest).header("Authorization"))
    }

    private companion object {
        const val AUTHORIZATION = "Bearer installation-token"
        const val GRAPHQL_RESPONSE =
            """{"data":{"repository":{"defaultBranchRef":{"target":{"history":{"nodes":[],"pageInfo":{"hasNextPage":false,"endCursor":null}}}}}}}"""
    }
}