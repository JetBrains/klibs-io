package io.klibs.integration.github

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.klibs.integration.github.configuration.properties.GitHubIntegrationProperties
import org.kohsuke.github.GHRateLimit
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider
import org.kohsuke.github.extras.authorization.JWTTokenProvider
import org.slf4j.LoggerFactory
import java.security.GeneralSecurityException
import java.util.Base64
import kotlin.system.measureTimeMillis
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GitHubIntegrationConfigurationTest {

    private val configLogger =
        LoggerFactory.getLogger(GitHubIntegrationConfiguration::class.java) as Logger
    private val logAppender = ListAppender<ILoggingEvent>()
    private var originalLevel: Level? = null

    @BeforeTest
    fun attachAppender() {
        logAppender.list.clear()
        logAppender.context = configLogger.loggerContext
        logAppender.start()
        configLogger.addAppender(logAppender)
        originalLevel = configLogger.level
        configLogger.level = Level.WARN
    }

    @AfterTest
    fun detachAppender() {
        configLogger.detachAppender(logAppender)
        configLogger.level = originalLevel
    }

    @Test
    fun `personal access token takes precedence over GitHub App configuration`() {
        val properties = properties(
            personalAccessToken = "personal-token",
            app = GitHubIntegrationProperties.App(
                clientId = "client-id",
                installationId = 42,
                privateKey = "not-used-because-token-takes-precedence",
            ),
        )

        val provider = GitHubIntegrationConfiguration().gitHubAuthorizationProvider(properties)

        assertEquals("token personal-token", provider.encodedAuthorization)
    }

    @Test
    fun `creates GitHub App provider when personal access token is blank`() {
        val app = GitHubIntegrationProperties.App(
            clientId = "client-id",
            installationId = 42,
            privateKey = createPrivateKey(),
        )

        val provider = GitHubIntegrationConfiguration().gitHubAuthorizationProvider(
            properties(personalAccessToken = " ", app = app),
        )

        assertIs<AppInstallationAuthorizationProvider>(provider)
    }

    @Test
    fun `generates GitHub App JWT authorization`() {
        val authorization = JWTTokenProvider("client-id", createPrivateKey()).encodedAuthorization

        assertTrue(authorization.startsWith("Bearer "))
        assertEquals(3, authorization.removePrefix("Bearer ").split('.').size)
    }

    @Test
    fun `rejects configuration without personal access token or GitHub App`() {
        val thrown = assertFailsWith<IllegalArgumentException> {
            GitHubIntegrationConfiguration().gitHubAuthorizationProvider(properties())
        }

        assertTrue(thrown.message.orEmpty().contains("GitHub authentication"))
    }

    @Test
    fun `rejects GitHub App configuration with blank client ID`() {
        val app = GitHubIntegrationProperties.App(
            clientId = " ",
            installationId = 42,
            privateKey = createPrivateKey(),
        )

        val thrown = assertFailsWith<IllegalArgumentException> {
            GitHubIntegrationConfiguration().gitHubAuthorizationProvider(properties(app = app))
        }

        assertTrue(thrown.message.orEmpty().contains("client ID"))
    }

    @Test
    fun `rejects GitHub App configuration with non-positive installation ID`() {
        val app = GitHubIntegrationProperties.App(
            clientId = "client-id",
            installationId = 0,
            privateKey = createPrivateKey(),
        )

        val thrown = assertFailsWith<IllegalArgumentException> {
            GitHubIntegrationConfiguration().gitHubAuthorizationProvider(properties(app = app))
        }

        assertTrue(thrown.message.orEmpty().contains("installation ID"))
    }

    @Test
    fun `rejects GitHub App configuration with blank private key`() {
        val app = GitHubIntegrationProperties.App(
            clientId = "client-id",
            installationId = 42,
            privateKey = " ",
        )

        val thrown = assertFailsWith<IllegalArgumentException> {
            GitHubIntegrationConfiguration().gitHubAuthorizationProvider(properties(app = app))
        }

        assertTrue(thrown.message.orEmpty().contains("private key"))
    }

    @Test
    fun `rejects GitHub App configuration with malformed private key`() {
        val app = GitHubIntegrationProperties.App(
            clientId = "client-id",
            installationId = 42,
            privateKey = "not-a-private-key",
        )

        assertFailsWith<GeneralSecurityException> {
            GitHubIntegrationConfiguration().gitHubAuthorizationProvider(properties(app = app))
        }
    }

    @Test
    fun `throws when used has reached fail threshold`() {
        val checker = newChecker()
        // limit=5000, remaining=500 → used=4500 → trips the guard before any 403.
        val record = GHRateLimit.Record(/* limit */ 5000, /* remaining */ 500, /* resetEpochSeconds */ 0L)

        val thrown = assertFailsWith<GitHubRateLimitExhaustedException> {
            checker.checkRateLimit(record, 0L)
        }
        val msg = thrown.message ?: ""
        assertTrue(msg.contains("used=4500"), "Message should mention used=4500: $msg")
        assertTrue(msg.contains("of 5000"), "Message should mention limit 5000: $msg")
    }

    @Test
    fun `logs WARN without sleeping when remaining is below warn threshold but used is below fail threshold`() {
        val checker = newChecker()
        // limit=5000, remaining=1000 → used=4000 → below fail, below warn (2000) → WARN only.
        val record = GHRateLimit.Record(/* limit */ 5000, /* remaining */ 1000, /* resetEpochSeconds */ 0L)

        val elapsedMs = measureTimeMillis {
            assertFalse(checker.checkRateLimit(record, 0L))
        }
        assertTrue(elapsedMs < 200, "Checker must not sleep; took ${elapsedMs}ms")

        val warns = logAppender.list.filter { it.level == Level.WARN }
        assertEquals(1, warns.size, "Expected exactly one WARN")
        val msg = warns[0].formattedMessage
        assertTrue(msg.contains("remaining=1000"), "WARN should mention remaining=1000: $msg")
        assertTrue(msg.contains("of 5000"), "WARN should mention limit 5000: $msg")
    }

    @Test
    fun `silent and non-sleeping when both used and remaining are healthy`() {
        val checker = newChecker()
        val record = GHRateLimit.Record(/* limit */ 5000, /* remaining */ 5000, /* resetEpochSeconds */ 0L)

        val elapsedMs = measureTimeMillis {
            assertFalse(checker.checkRateLimit(record, 0L))
        }
        assertTrue(elapsedMs < 200, "Checker must not sleep; took ${elapsedMs}ms")
        assertTrue(
            logAppender.list.none { it.level == Level.WARN },
            "No WARN expected when remaining is above threshold",
        )
    }

    private fun newChecker() =
        GitHubIntegrationConfiguration.FailingRateLimitChecker(failAtUsed = 4500, warnAtUsed = 2000)

    private fun properties(
        personalAccessToken: String? = null,
        app: GitHubIntegrationProperties.App? = null,
    ) = GitHubIntegrationProperties(
        personalAccessToken = personalAccessToken,
        app = app,
        cache = GitHubIntegrationProperties.Cache(),
        webhook = GitHubIntegrationProperties.Webhook(),
        indexRequests = GitHubIntegrationProperties.IndexRequests(),
    )

    private fun createPrivateKey(): String {
        val keyPair = java.security.KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val encoded = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded)
        return "-----BEGIN PRIVATE KEY-----\n$encoded\n-----END PRIVATE KEY-----\n"
    }
}
