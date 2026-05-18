package io.klibs.integration.maven.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Stores timestamps for periodic indexing jobs.
 *
 * The table holds exactly one row (with [id] = 1) and saves
 * progress for two independent processes:
 *  - the Maven Central indexing job
 *  - the GitHub index-request issues polling job
 */
@Entity
@Table(name = "maven_central_log")
class MavenCentralLogEntity(
    @Id
    @Column(name = "id")
    val id: Int = 1,

    /**
     * Timestamp indicating the latest processed .index from Maven Central
     */
    @Column(name = "maven_index_timestamp")
    var mavenIndexTimestamp: Instant,

    /**
     * Timestamp of the last successful poll of GitHub index-request issues.
     * Updated only when a polling run completes without any server errors
     */
    @Column(name = "user_request_check_timestamp")
    var userRequestCheckTimestamp: Instant

)