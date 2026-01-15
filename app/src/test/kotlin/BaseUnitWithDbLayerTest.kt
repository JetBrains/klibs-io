import io.awspring.cloud.s3.S3Template
import io.klibs.app.Application
import io.klibs.integration.ai.AiService
import org.springframework.ai.model.openai.autoconfigure.*
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase
import org.springframework.test.context.jdbc.SqlMergeMode
import org.testcontainers.containers.PostgreSQLContainer

@ActiveProfiles("test")
@SpringBootTest(classes = [Application::class])
@EnableAutoConfiguration(exclude = [
    OpenAiChatAutoConfiguration::class,
    OpenAiAudioTranscriptionAutoConfiguration::class,
    OpenAiAudioSpeechAutoConfiguration::class,
    OpenAiEmbeddingAutoConfiguration::class,
    OpenAiImageAutoConfiguration::class,
    OpenAiModerationAutoConfiguration::class,
])
@TestPropertySource(properties = ["spring.sql.init.mode=NEVER"])
@Sql(value = ["classpath:sql/truncate.sql"], executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
@SqlMergeMode(value = SqlMergeMode.MergeMode.MERGE)
@AutoConfigureMockMvc
abstract class BaseUnitWithDbLayerTest {

    @MockBean
    protected lateinit var aiService: AiService

    @MockBean
    protected lateinit var s3Template: S3Template

    companion object {
        val postgresContainer: PostgreSQLContainer<Nothing> by lazy {
            PostgreSQLContainer<Nothing>("postgres:17.0").apply {
                withDatabaseName("testdb")
                withUsername("testuser")
                withPassword("testpass")
                start()
            }
        }

        init {
            postgresContainer
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
            registry.add("klibs.readme.s3.bucket-name") { "test-bucket" }
            registry.add("klibs.readme.s3.prefix") { "readme" }
            registry.add("klibs.integration.github.cache.request-cache-path") { "build/tmp/gh-req-cache" }
        }
    }
}
