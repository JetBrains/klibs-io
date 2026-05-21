package io.klibs.integration.github

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.kohsuke.github.GHRateLimit
import org.slf4j.LoggerFactory
import kotlin.system.measureTimeMillis
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
}
