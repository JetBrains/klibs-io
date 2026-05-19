package io.klibs.integration.maven.entity

import io.klibs.integration.maven.dto.MavenCentralLogType
import jakarta.persistence.*
import java.time.Instant

/**
 * Stores timestamps for periodic indexing jobs.
 *
 * For the Maven Central indexing job:
 *      Timestamp indicating the latest processed .index from Maven Central.
 * For the GitHub index-request issues polling job:
 *      Timestamp of the last successful poll of GitHub index-request issues,
 *      updated only when a polling run completes without any server errors.
 */
@Entity
@Table(name = "maven_central_log")
class MavenCentralLogEntity(
    @Id
    @Column(name = "id")
    val id: Int = 1,

    @Enumerated(EnumType.STRING)
    @Column(name = "log_type", nullable = false, unique = true)
    val logType: MavenCentralLogType,

    @Column(name = "last_run_at", nullable = false)
    val timestamp: Instant,
)
