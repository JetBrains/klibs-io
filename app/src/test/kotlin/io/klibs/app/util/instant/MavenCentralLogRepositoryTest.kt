package io.klibs.app.util.instant

import BaseUnitWithDbLayerTest
import io.klibs.integration.maven.repository.MavenCentralLogRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.springframework.transaction.annotation.Transactional

class MavenCentralLogRepositoryTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var repository: MavenCentralLogRepository

    @Test
    @Transactional
    fun `should save and retrieve maven index timestamp`() {
        val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        repository.saveMavenIndexTimestamp(now)
        
        val retrieved = repository.retrieveMavenIndexTimestamp()
        assertNotNull(retrieved)
        assertEquals(now, retrieved)
    }
}
