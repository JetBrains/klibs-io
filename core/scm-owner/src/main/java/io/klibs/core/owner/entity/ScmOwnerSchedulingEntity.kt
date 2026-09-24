package io.klibs.core.owner.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "scm_owner_scheduling")
class ScmOwnerSchedulingEntity(

    @Id
    @Column(name = "scm_owner_id")
    val scmOwnerId: Int,

    @Column(name = "next_retry_at")
    val nextRetryAt: Instant?,

    @Column(name = "retry_attempts")
    val retryAttempts: Int,

    @Column(name = "reason")
    val reason: String,
)
