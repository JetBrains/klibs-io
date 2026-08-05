import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

/**
 * Adds a single-node OpenSearch alongside the PostgreSQL container of [BaseUnitWithDbLayerTest] and
 * points the app at it.
 */
abstract class BaseOpenSearchTest : BaseUnitWithDbLayerTest() {

    class OpenSearchContainer(image: String) : GenericContainer<OpenSearchContainer>(image)

    companion object {
        const val OPENSEARCH_PORT = 9200

        val openSearchContainer: OpenSearchContainer by lazy {
            OpenSearchContainer("opensearchproject/opensearch:3.7.0").apply {
                withExposedPorts(OPENSEARCH_PORT)
                withEnv("discovery.type", "single-node")
                withEnv("DISABLE_SECURITY_PLUGIN", "true")
                withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
                withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
                // Deploy-owned in prod (opensearch.yml via Helm, M0). Set here too, or the guard
                // that turns a mapping-less auto-created index into a failed run ships untested.
                withEnv("action.auto_create_index", "-project*,-package*,+*")
                waitingFor(Wait.forHttp("/_cluster/health").forStatusCode(200))
                withStartupTimeout(Duration.ofMinutes(3))
                start()
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun openSearchProperties(registry: DynamicPropertyRegistry) {
            registry.add("klibs.search.opensearch.enabled") { "true" }
            registry.add("klibs.search.opensearch.uri") {
                "http://${openSearchContainer.host}:${openSearchContainer.getMappedPort(OPENSEARCH_PORT)}"
            }
            // Generations in a test are seconds old; the 1h production guard would reap nothing.
            registry.add("klibs.search.opensearch.reap-min-age") { "0s" }
        }
    }
}
