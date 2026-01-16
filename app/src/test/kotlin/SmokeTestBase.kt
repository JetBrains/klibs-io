import io.awspring.cloud.s3.S3Template
import io.klibs.app.Application
import io.klibs.integration.ai.AiService
import org.springframework.ai.model.openai.autoconfigure.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import com.ninjasquad.springmockk.MockkBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.PostgreSQLContainer

@SpringBootTest(classes = [Application::class])
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = [
    OpenAiChatAutoConfiguration::class,
    OpenAiAudioTranscriptionAutoConfiguration::class,
    OpenAiAudioSpeechAutoConfiguration::class,
    OpenAiEmbeddingAutoConfiguration::class,
    OpenAiImageAutoConfiguration::class,
    OpenAiModerationAutoConfiguration::class,
])
@Import(SmokeTestBase.TestConfig::class)
abstract class SmokeTestBase {

    @MockkBean
    private lateinit var aiService: AiService

    @MockkBean
    private lateinit var s3Template: S3Template

    @Autowired
    protected lateinit var mockMvc: MockMvc

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

    @TestConfiguration
    class TestConfig {

        @Bean
        @Primary
        fun jdbcClient(): JdbcClient {
            return JdbcClient.create(DriverManagerDataSource().apply {
                setDriverClassName("org.postgresql.Driver")
                url = postgresContainer.jdbcUrl
                username = postgresContainer.username
                password = postgresContainer.password
            })
        }
    }
}
