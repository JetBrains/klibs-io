package io.klibs.integration.maven.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * Stores information about the updates from the Maven Central.
 * It has only one log entry and represents the state of the last processed index and package.
 */
@Entity
@Table(name = "maven_central_log")
class MavenCentralLogEntity(
    @Id
    @Column(name = "id")
    val id: Int = 1,

    /**
     * Timestamp indicating the latest processed .index
     */
    @Column(name = "maven_index_timestamp")
    var mavenIndexTimestamp: Instant
)