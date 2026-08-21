package io.klibs.app.job

import io.klibs.core.pckg.service.SuspiciousPackagePairCollector
import net.javacrumbs.shedlock.core.LockAssert
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class CollectSuspiciousPackagePairsJobTest {

    @BeforeEach
    fun bypassLockAssert() {
        LockAssert.TestHelper.makeAllAssertsPass(true)
    }

    @AfterEach
    fun restoreLockAssert() {
        LockAssert.TestHelper.makeAllAssertsPass(false)
    }

    @Test
    fun `scheduled run recomputes the candidate collection`() {
        val collector = mock<SuspiciousPackagePairCollector>()
        val job = CollectSuspiciousPackagePairsJob(collector)

        job.collectSuspiciousPackagePairs()

        verify(collector).recompute()
    }
}
