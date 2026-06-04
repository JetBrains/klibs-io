package io.klibs.integration.maven.repository

import io.klibs.integration.maven.dto.MavenCentralLogType
import io.klibs.integration.maven.entity.MavenCentralLogEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Repository
interface MavenCentralLogRepository : JpaRepository<MavenCentralLogEntity, Int> {

    @Transactional
    @Modifying
    @Query("UPDATE MavenCentralLogEntity l SET l.timestamp = :instant WHERE l.logType = :type")
    fun saveTimestamp(@Param("type") type: MavenCentralLogType, @Param("instant") instant: Instant)

    @Query("SELECT l.timestamp FROM MavenCentralLogEntity l WHERE l.logType = :type")
    fun retrieveTimestamp(@Param("type") type: MavenCentralLogType): Instant
}