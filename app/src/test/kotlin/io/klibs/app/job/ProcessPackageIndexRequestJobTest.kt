package io.klibs.app.job

import io.klibs.app.configuration.properties.ProcessPackageIndexingQueueProperties
import io.klibs.app.indexing.PackageIndexingService
import net.javacrumbs.shedlock.core.LockAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Duration

class ProcessPackageIndexRequestJobTest {

    private val packageIndexingService = mock<PackageIndexingService>()
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
        whenever(packageIndexingService.processPackageQueue()).thenReturn(false)

        uut.processPackageIndexRequests()

        verify(packageIndexingService, times(1)).processPackageQueue()
    }

    @Test
    fun `processPackageIndexQueue stops when queue is empty`() {
        whenever(packageIndexingService.processPackageQueue()).thenReturn(false)

        val processed = uut.processPackageIndexQueue()

        assertEquals(0, processed)
        verify(packageIndexingService, times(1)).processPackageQueue()
    }

    @Test
    fun `processPackageIndexQueue stops when deadline is exceeded`() {
        var currentTime = 1000L
        uut.timeProvider = { currentTime }

        whenever(packageIndexingService.processPackageQueue()).thenAnswer {
            currentTime += 600L
            true
        }

        val processed = uut.processPackageIndexQueue(deadline = 2000L)

        assertEquals(2, processed)
        verify(packageIndexingService, times(2)).processPackageQueue()
    }

    @Test
    fun `processPackageIndexQueue does not pick requests if deadline already passed`() {
        uut.timeProvider = { 5000L }

        val processed = uut.processPackageIndexQueue(deadline = 4000L)

        assertEquals(0, processed)
        verify(packageIndexingService, never()).processPackageQueue()
    }

    @Test
    fun `default properties have expected defaults`() {
        val properties = ProcessPackageIndexingQueueProperties()

        assertTrue(properties.enabled)
        assertEquals(Duration.ofMinutes(29), properties.deadline)
        assertEquals(Duration.ofMinutes(30), properties.fixedRate)
    }
}
