package io.klibs.core.search.opensearch

import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.core5.http.HttpHost
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI

/**
 * Wires the typed [OpenSearchClient]. Active only when `klibs.search.opensearch.enabled=true`
 * so normal app boot / non-OS tests are unaffected. Basic auth applied when credentials are set.
 */
@Configuration
@EnableConfigurationProperties(OpenSearchProperties::class)
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class OpenSearchConfiguration {

    @Bean
    fun openSearchClient(properties: OpenSearchProperties): OpenSearchClient {
        val uri = URI(properties.uri)
        val host = HttpHost(uri.scheme, uri.host, uri.port)
        val transport = ApacheHttpClient5TransportBuilder.builder(host)
            .setMapper(JacksonJsonpMapper())
            .apply {
                val user = properties.username
                val pass = properties.password
                if (!user.isNullOrBlank() && pass != null) {
                    val creds = BasicCredentialsProvider().apply {
                        setCredentials(AuthScope(host), UsernamePasswordCredentials(user, pass.toCharArray()))
                    }
                    setHttpClientConfigCallback { it.setDefaultCredentialsProvider(creds) }
                }
            }
            .build()
        return OpenSearchClient(transport)
    }
}
