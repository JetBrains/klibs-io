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
        const val OPENSEARCH_USERNAME = "admin"
        const val OPENSEARCH_PASSWORD = "OpenSearch!ocalPassw0rd"

        val openSearchContainer: OpenSearchContainer by lazy {
            OpenSearchContainer("opensearchproject/opensearch:3.7.0").apply {
                withExposedPorts(OPENSEARCH_PORT)
                withEnv("discovery.type", "single-node")
                // Built-in demo security: HTTPS:9200 + an `admin` user, mirroring the prod TLS/auth path.
                withEnv("OPENSEARCH_INITIAL_ADMIN_PASSWORD", OPENSEARCH_PASSWORD)
                withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
                // Deploy-owned in prod (opensearch.yml via Helm, M0). Set here too, or the guard
                // that turns a mapping-less auto-created index into a failed run ships untested.
                withEnv("action.auto_create_index", "-klibs-project*,-klibs-package*,+*")
                waitingFor(
                    Wait.forHttp("/_cluster/health")
                        .usingTls()
                        .allowInsecure()
                        .withBasicCredentials(OPENSEARCH_USERNAME, OPENSEARCH_PASSWORD)
                        .forStatusCode(200)
                )
                withStartupTimeout(Duration.ofMinutes(3))
                start()
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun openSearchProperties(registry: DynamicPropertyRegistry) {
            registry.add("klibs.search.opensearch.enabled") { "true" }
            registry.add("klibs.search.opensearch.uri") {
                "https://${openSearchContainer.host}:${openSearchContainer.getMappedPort(OPENSEARCH_PORT)}"
            }
            registry.add("klibs.search.opensearch.username") { OPENSEARCH_USERNAME }
            registry.add("klibs.search.opensearch.password") { OPENSEARCH_PASSWORD }
            registry.add("klibs.search.opensearch.trust-all-certificates") { "true" }
        }
    }
}
