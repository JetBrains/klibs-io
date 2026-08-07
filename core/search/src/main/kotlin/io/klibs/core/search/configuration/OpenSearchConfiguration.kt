package io.klibs.core.search.configuration

import io.klibs.core.search.configuration.properties.OpenSearchProperties
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.util.Timeout
import org.opensearch.client.json.jackson.JacksonJsonpMapper
import org.opensearch.client.opensearch.OpenSearchClient
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

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
    fun openSearchClient(properties: OpenSearchProperties, mapper: JacksonJsonpMapper): OpenSearchClient {
        val uri = URI(properties.uri)
        val host = HttpHost(uri.scheme, uri.host, uri.port)
        val connectTimeout = Timeout.of(properties.connectTimeout)
        val socketTimeout = Timeout.of(properties.socketTimeout)
        val requestTimeout = Timeout.of(properties.requestTimeout)
        val user = properties.username
        val pass = properties.password
        val credentials = if (!user.isNullOrBlank() && pass != null) {
            BasicCredentialsProvider().apply {
                setCredentials(AuthScope(host), UsernamePasswordCredentials(user, pass.toCharArray()))
            }
        } else {
            null
        }
        val transport = ApacheHttpClient5TransportBuilder.builder(host)
            .setMapper(mapper)
            .setRequestConfigCallback {
                it.setConnectionRequestTimeout(requestTimeout)
                    .setResponseTimeout(requestTimeout)
            }
            .setConnectionConfigCallback {
                it.setConnectTimeout(connectTimeout)
                    .setSocketTimeout(socketTimeout)
            }
            .setHttpClientConfigCallback { httpClient ->
                // We always use https, so always install a TLS strategy.
                val tlsStrategyBuilder = ClientTlsStrategyBuilder.create()
                if (properties.trustAllCertificates) {
                    tlsStrategyBuilder
                        .setSslContext(trustAllSslContext())
                        .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                }
                httpClient.setConnectionManager(
                    PoolingAsyncClientConnectionManagerBuilder.create()
                        .setTlsStrategy(tlsStrategyBuilder.build())
                        .build()
                )
                credentials?.let { httpClient.setDefaultCredentialsProvider(it) }
                httpClient
            }
            .build()
        return OpenSearchClient(transport)
    }

    /** Trust-all [SSLContext] for local dev over https against a self-signed cert. Never enable in prod. */
    private fun trustAllSslContext(): SSLContext {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), SecureRandom())
        }
    }
}
