package io.klibs.core.search.configuration

import io.klibs.core.search.configuration.properties.OpenSearchProperties
import io.klibs.core.search.dto.opensearch.OpenSearchIndexSpec
import io.klibs.core.search.opensearch.IndexDefinitions
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.config.ConnectionConfig
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.ssl.SSLContextBuilder
import org.apache.hc.core5.util.Timeout
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.ssl.SslBundles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI
import javax.net.ssl.SSLContext

/**
 * Wires the typed [OpenSearchClient]. Active only when `klibs.search.opensearch.enabled=true`
 * so normal app boot / non-OS tests are unaffected. Basic auth applied when credentials are set.
 */
@Configuration
@EnableConfigurationProperties(OpenSearchProperties::class)
@ConditionalOnProperty("klibs.search.opensearch.enabled", havingValue = "true")
class OpenSearchConfiguration {

    @Bean
    fun openSearchJsonpMapper(): JacksonJsonpMapper = JacksonJsonpMapper()

    @Bean
    fun projectIndexSpec(properties: OpenSearchProperties): OpenSearchIndexSpec = OpenSearchIndexSpec(
        base = properties.projectIndex,
        settings = IndexDefinitions.PROJECT_SETTINGS,
        mappings = IndexDefinitions.PROJECT_MAPPINGS,
        sql = IndexDefinitions.PROJECT_DOC_SQL,
    ) { it.get("project_id").asText() }

    @Bean
    fun packageIndexSpec(properties: OpenSearchProperties): OpenSearchIndexSpec = OpenSearchIndexSpec(
        base = properties.packageIndex,
        settings = IndexDefinitions.PACKAGE_SETTINGS,
        mappings = IndexDefinitions.PACKAGE_MAPPINGS,
        sql = IndexDefinitions.PACKAGE_DOC_SQL,
    ) { "${it.get("group_id").asText()}:${it.get("artifact_id").asText()}" }

    @Bean
    fun openSearchClient(
        properties: OpenSearchProperties,
        mapper: JacksonJsonpMapper,
        sslBundles: SslBundles,
    ): OpenSearchClient {
        val uri = URI(properties.uri)
        val host = HttpHost(uri.scheme, uri.host, uri.port)
        val requestTimeout = Timeout.of(properties.requestTimeout)
        if (sslBundles.bundleNames.contains(OPENSEARCH_SSL_BUNDLE)) {
            SSLContext.setDefault(sslBundles.getBundle(OPENSEARCH_SSL_BUNDLE).createSslContext())
        }
        val transport = ApacheHttpClient5TransportBuilder.builder(host)
            .setMapper(mapper)
            // Connect and socket timeouts are left to the transport's own ConnectionConfig defaults
            // (1s / 30s); only the pool-checkout wait needs overriding, see requestTimeout.
            .setRequestConfigCallback { it.setConnectionRequestTimeout(requestTimeout) }
            .apply {
                val user = properties.username
                val pass = properties.password
                setHttpClientConfigCallback { httpClient ->
                    if (!user.isNullOrBlank() && pass != null) {
                        val creds = BasicCredentialsProvider().apply {
                            setCredentials(AuthScope(host), UsernamePasswordCredentials(user, pass.toCharArray()))
                        }
                        httpClient.setDefaultCredentialsProvider(creds)
                    }
                    if (properties.trustAllCertificates) {
                        httpClient.setConnectionManager(trustAllConnectionManager())
                    }
                    httpClient
                }
            }
            .build()
        return OpenSearchClient(transport)
    }

    /**
     * Builds a trust-all async connection manager mirroring the defaults that
     * `ApacheHttpClient5TransportBuilder` sets internally
     */
    private fun trustAllConnectionManager() = PoolingAsyncClientConnectionManagerBuilder.create()
        .setMaxConnPerRoute(MAX_CONN_PER_ROUTE)
        .setMaxConnTotal(MAX_CONN_TOTAL)
        .setDefaultConnectionConfig(
            ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(CONNECTION_TIMEOUT))
                .setSocketTimeout(Timeout.ofSeconds(SOCKET_TIMEOUT))
                .build()
        )
        .setTlsStrategy(
            ClientTlsStrategyBuilder.create()
                .setSslContext(SSLContextBuilder.create().loadTrustMaterial(TrustAllStrategy.INSTANCE).build())
                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .buildAsync()
        )
        .build()

    private companion object {
        const val OPENSEARCH_SSL_BUNDLE = "opensearch"
        const val MAX_CONN_PER_ROUTE = 10
        const val MAX_CONN_TOTAL = 30
        const val CONNECTION_TIMEOUT = 1L
        const val SOCKET_TIMEOUT = 30L
    }
}
