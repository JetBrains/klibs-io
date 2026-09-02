package io.klibs.core.search.eval

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.test.context.DynamicPropertyRegistrar
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

@TestConfiguration(proxyBeanMethods = false)
class SearchEvalOpenSearchConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    @Lazy
    fun searchEvalOpenSearch(): GenericContainer<*> =
        GenericContainer<Nothing>(IMAGE).apply {
            withExposedPorts(PORT)
            withEnv("discovery.type", "single-node")
            // Built-in demo security: HTTPS:9200 + an `admin` user, mirroring the prod TLS/auth path.
            withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", PASSWORD)
            withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
            waitingFor(
                Wait.forHttps("/_cluster/health")
                    .forPort(PORT)
                    .allowInsecure()
                    .withBasicCredentials(USERNAME, PASSWORD)
                    .withStartupTimeout(STARTUP_TIMEOUT),
            )
            log.info("starting {} for search-eval", IMAGE)
        }

    /** Connection properties: resolving the URI is what starts the container. */
    @Bean
    fun searchEvalOpenSearchProperties(container: ObjectProvider<GenericContainer<*>>) =
        DynamicPropertyRegistrar { registry ->
            registry.add("klibs.search.opensearch.trust-all-certificates") { "true" }
            registry.add("klibs.search.opensearch.username") { USERNAME }
            registry.add("klibs.search.opensearch.password") { PASSWORD }
            registry.add("klibs.search.opensearch.uri") { uri(container) }
        }

    private fun uri(container: ObjectProvider<GenericContainer<*>>): String {
        val external = System.getenv(URI_ENV)?.takeIf { it.isNotBlank() }
        if (external != null) {
            log.info("using the external OpenSearch at {} — image and index state are not pinned", external)
            return external
        }
        val started = container.getObject()
        return "https://${started.host}:${started.getMappedPort(PORT)}"
    }

    companion object {
        const val USERNAME = "admin"
        const val PASSWORD = "OpenSearch!ocalPassw0rd"

        private const val URI_ENV = "SEARCH_EVAL_OS_URI"
        private const val IMAGE = "opensearchproject/opensearch:3.7.0"
        private const val PORT = 9200
        private val STARTUP_TIMEOUT: Duration = Duration.ofMinutes(3)

        private val log = LoggerFactory.getLogger(SearchEvalOpenSearchConfig::class.java)
    }
}
