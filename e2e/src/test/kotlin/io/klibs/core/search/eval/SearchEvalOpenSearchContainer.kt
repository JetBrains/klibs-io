package io.klibs.core.search.eval

import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

object SearchEvalOpenSearchContainer {

    const val USERNAME = "admin"
    const val PASSWORD = "OpenSearch!ocalPassw0rd"

    private const val IMAGE = "opensearchproject/opensearch:3.7.0"
    private const val PORT = 9200
    private val STARTUP_TIMEOUT: Duration = Duration.ofMinutes(3)

    private val log = LoggerFactory.getLogger(SearchEvalOpenSearchContainer::class.java)

    private val container: GenericContainer<*> by lazy {
        GenericContainer(IMAGE)
            .withExposedPorts(PORT)
            .withEnv("discovery.type", "single-node")
            // Built-in demo security: HTTPS:9200 + an `admin` user, mirroring the prod TLS/auth path.
            .withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", PASSWORD)
            .withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
            .waitingFor(
                Wait.forHttps("/_cluster/health")
                    .forPort(PORT)
                    .allowInsecure()
                    .withBasicCredentials(USERNAME, PASSWORD)
                    .withStartupTimeout(STARTUP_TIMEOUT),
            )
            .apply {
                log.info("starting {} for search-eval", IMAGE)
                start()
            }
    }

    fun uriOrDefault(override: String?): String {
        if (override.isNullOrBlank()) return uri()
        log.info("using the external OpenSearch at {} — image and index state are not pinned", override)
        return override
    }

    fun uri(): String = "https://${container.host}:${container.getMappedPort(PORT)}"
}
