package io.klibs.integration.github

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class GitHubRequestMeteringInterceptorTest {

    private val registry = SimpleMeterRegistry()
    private val interceptor = GitHubRequestMeteringInterceptor(registry)

    @Test
    fun `tags REST requests by inferred origin`() {
        intercept("https://api.github.com/repos/kotlin/dokka/issues")
        intercept("https://api.github.com/repos/kotlin/dokka/pulls?state=all")
        intercept("https://api.github.com/repos/kotlin/dokka/topics")
        intercept("https://api.github.com/repos/kotlin/dokka/stats/participation")
        intercept("https://api.github.com/repos/kotlin/dokka/readme")
        intercept("https://api.github.com/repos/kotlin/dokka/license")
        intercept("https://api.github.com/repos/kotlin/dokka")
        intercept("https://api.github.com/repositories/21763603")
        intercept("https://api.github.com/users/bnorm")
        intercept("https://api.github.com/licenses/mit")
        intercept("https://api.github.com/markdown")
        intercept("https://api.github.com/rate_limit")

        assertOrigin("rest", "issues", 1.0)
        assertOrigin("rest", "prs", 1.0)
        assertOrigin("rest", "topics", 1.0)
        assertOrigin("rest", "stats-participation", 1.0)
        // /repos/.../readme and /repos/.../license both resolve to their endpoint origin.
        assertOrigin("rest", "readme", 1.0)
        assertOrigin("rest", "license", 2.0) // /repos/.../license + /licenses/mit
        assertOrigin("rest", "repository", 2.0) // /repos/owner/name + /repositories/{id}
        assertOrigin("rest", "user", 1.0)
        assertOrigin("rest", "markdown", 1.0)
        assertOrigin("rest", "rate-limit", 1.0)
    }

    @Test
    fun `flags graphql requests under api=graphql with origin=graphql`() {
        intercept("https://api.github.com/graphql")

        assertOrigin("graphql", "graphql", 1.0)
    }

    @Test
    fun `infers readme origin from repositories-readme path`() {
        intercept("https://api.github.com/repositories/21763603/readme")

        assertOrigin("rest", "readme", 1.0)
    }

    @Test
    fun `falls back to other for unknown paths`() {
        intercept("https://api.github.com/something/weird")

        assertOrigin("rest", "other", 1.0)
    }

    private fun intercept(url: String) {
        interceptor.intercept(FakeChain(Request.Builder().url(url).get().build()))
    }

    private fun assertOrigin(api: String, origin: String, expected: Double) {
        val counter = registry.find(GitHubRequestMeteringInterceptor.METER_NAME)
            .tag("api", api)
            .tag("origin", origin)
            .counter()
        assertEquals(expected, counter?.count() ?: 0.0, "api=$api origin=$origin")
    }

    private class FakeChain(private val req: Request) : Interceptor.Chain {
        override fun request(): Request = req
        override fun proceed(request: Request): Response =
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody(null))
                .build()
        override fun call(): Call = throw UnsupportedOperationException()
        override fun connection(): Connection? = null
        override fun connectTimeoutMillis(): Int = 0
        override fun readTimeoutMillis(): Int = 0
        override fun writeTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
        override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit): Interceptor.Chain = this
    }
}
