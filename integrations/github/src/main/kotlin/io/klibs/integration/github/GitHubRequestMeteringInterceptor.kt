package io.klibs.integration.github

import io.micrometer.core.instrument.MeterRegistry
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Counts every outgoing HTTP request to the GitHub API as `klibs.github.requests`.
 *
 * Labels:
 *  - `api`    — `rest` or `graphql`. The same Bearer token is rate-limited independently
 *               on REST and GraphQL, so they need separate budgets.
 *  - `origin` — call-site identifier inferred from the URL path, e.g. `issues`, `prs`,
 *               `stats-participation`. Mirrors the Micrometer `uriMapper` pattern
 *               (`OkHttpMetricsEventListener.uriMapper`) — we map dynamic URL paths to a
 *               bounded label set so cardinality stays sane.
 *
 * Placed on the shared [okhttp3.OkHttpClient] bean, so it sees both kohsuke-driven traffic
 * (via `OkHttpGitHubConnector`) and the direct `okHttpClient.newCall(...)` paths (README,
 * GraphQL). Pagination, internal retries, and redirect-follows produce one HTTP call each
 * and increment the counter once each.
 */
class GitHubRequestMeteringInterceptor(
    private val meterRegistry: MeterRegistry,
) : Interceptor {

    enum class Api(val label: String) {
        REST("rest"),
        GRAPHQL("graphql"),
    }

    enum class Origin(val label: String) {
        REPOSITORY("repository"),
        USER("user"),
        LICENSE("license"),
        README("readme"),
        MARKDOWN("markdown"),
        TOPICS("topics"),
        ISSUES("issues"),
        PRS("prs"),
        STATS_PARTICIPATION("stats-participation"),
        RATE_LIMIT("rate-limit"),
        GRAPHQL("graphql"),
        OTHER("other"),
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        val api = if (request.url.encodedPath == "/graphql") Api.GRAPHQL else Api.REST
        val origin = inferOrigin(request.url)
        meterRegistry.counter(METER_NAME, "api", api.label, "origin", origin.label).increment()
        return response
    }

    private fun inferOrigin(url: HttpUrl): Origin {
        val s = url.pathSegments
        return when {
            s.size >= 4 && s[0] == "repos" -> when (s[3]) {
                "issues" -> Origin.ISSUES
                "pulls" -> Origin.PRS
                "topics" -> Origin.TOPICS
                "stats" -> if (s.getOrNull(4) == "participation") Origin.STATS_PARTICIPATION else Origin.OTHER
                "readme" -> Origin.README
                "license" -> Origin.LICENSE
                else -> Origin.OTHER
            }
            s.size == 3 && s[0] == "repos" -> Origin.REPOSITORY
            s.size >= 2 && s[0] == "repositories" -> when (s.getOrNull(2)) {
                null, "" -> Origin.REPOSITORY
                "readme" -> Origin.README
                "license" -> Origin.LICENSE
                else -> Origin.OTHER
            }
            s.size == 2 && s[0] == "users" -> Origin.USER
            s.size == 2 && s[0] == "licenses" -> Origin.LICENSE
            url.encodedPath == "/markdown" || url.encodedPath.startsWith("/markdown/") -> Origin.MARKDOWN
            url.encodedPath == "/rate_limit" -> Origin.RATE_LIMIT
            url.encodedPath == "/graphql" -> Origin.GRAPHQL
            else -> Origin.OTHER
        }
    }

    companion object {
        const val METER_NAME = "klibs.github.requests"
    }
}
