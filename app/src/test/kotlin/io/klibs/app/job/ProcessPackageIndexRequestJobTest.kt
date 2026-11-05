package io.klibs.app.job

import io.klibs.app.configuration.properties.ProcessPackageIndexingQueueProperties
import io.klibs.app.indexing.PackageIndexingService
import net.javacrumbs.shedlock.core.LockAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration

class ProcessPackageIndexRequestJobTest {

    private val packageIndexingService = mockk<PackageIndexingService>()
    private val uut = ProcessPackageIndexRequestJob(packageIndexingService)

    @BeforeEach
    fun setUp() {
        LockAssert.TestHelper.makeAllAssertsPass(true)
    }

    @AfterEach
    fun tearDown() {
        LockAssert.TestHelper.makeAllAssertsPass(false)
    }

    @Test
    fun `processPackageIndexRequests executes when locked and stops when queue is empty`() {
        every { packageIndexingService.processPackageQueue() } returns false

        uut.processPackageIndexRequests()

        verify(exactly = 1) { packageIndexingService.processPackageQueue() }
    }

    @Test
    fun `processPackageIndexQueue stops when queue is empty`() {
        every { packageIndexingService.processPackageQueue() } returns false

        val processed = uut.processPackageIndexQueue()

        assertEquals(0, processed)
        verify(exactly = 1) { packageIndexingService.processPackageQueue() }
    }

    @Test
    fun `processPackageIndexQueue stops when deadline is exceeded`() {
        var currentTime = 1000L
        uut.timeProvider = { currentTime }

        every { packageIndexingService.processPackageQueue() } answers {
            currentTime += 600L
            true
        }

        val processed = uut.processPackageIndexQueue(deadline = 2000L)

        assertEquals(2, processed)
        verify(exactly = 2) { packageIndexingService.processPackageQueue() }
    }

    @Test
    fun `processPackageIndexQueue does not pick requests if deadline already passed`() {
        uut.timeProvider = { 5000L }

        val processed = uut.processPackageIndexQueue(deadline = 4000L)

        assertEquals(0, processed)
        verify(exactly = 0) { packageIndexingService.processPackageQueue() }
    }

    @Test
    fun `default properties have expected defaults`() {
        val properties = ProcessPackageIndexingQueueProperties()

        assertTrue(properties.enabled)
        assertEquals(Duration.ofMinutes(30), properties.fixedRate)
    }
}
