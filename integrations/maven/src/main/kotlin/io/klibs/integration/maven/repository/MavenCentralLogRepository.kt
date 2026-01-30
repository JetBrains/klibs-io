package io.klibs.integration.maven.repository

import io.klibs.integration.maven.entity.MavenCentralLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface MavenCentralLogRepository : JpaRepository<MavenCentralLogEntity, Int> {
    @Transactional
    @Modifying
    @Query("UPDATE MavenCentralLogEntity l SET l.mavenIndexTimestamp = :instant WHERE l.id = 1")
    fun saveMavenIndexTimestamp(instant: Instant)

    @Query("SELECT l.mavenIndexTimestamp FROM MavenCentralLogEntity l WHERE l.id = 1")
    fun retrieveMavenIndexTimestamp(): Instant
}