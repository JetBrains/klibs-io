package io.klibs.core.pckg.service

import io.klibs.core.pckg.dto.projection.SuspiciousPackagePairView
import io.klibs.core.pckg.entity.SuspiciousPackagePairCandidateEntity
import io.klibs.core.pckg.enums.CandidateStatus
import io.klibs.core.pckg.repository.SuspiciousPackagePairCandidateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class SuspiciousPackagePairCollector(
    private val repository: SuspiciousPackagePairCandidateRepository,
) {

    /**
     * Upserts current conflicts on the unique `(project_id, artifact_id, group_id)` key
     * new ones inserted `PENDING`; existing rows have only signal columns refreshed
     */
    @Transactional
    fun recompute() {
        val detectedAt = Instant.now()
        val detected = repository.findConflictingPairs()
        val existingByKey = repository.findAll().associateBy { keyOf(it) }

        var inserted = 0
        val rows = detected.map { view ->
            val existing = existingByKey[keyOf(view)]
            if (existing == null) inserted++
            merge(existing, view, deriveOwnerLogin(view.groupId), detectedAt)
        }
        repository.saveAll(rows)

        logger.info(
            "Suspicious package-pair recompute: inserted={}, signalUpdated={}, total={}",
            inserted, rows.size - inserted, rows.size,
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SuspiciousPackagePairCollector::class.java)
        private val GITHUB_OWNER = Regex("^(?:io|com)\\.github\\.([^.]+)")

        /** `io.github.*` / `com.github.*` -> lower-cased owner segment; anything else -> null. */
        fun deriveOwnerLogin(groupId: String): String? =
            GITHUB_OWNER.find(groupId)?.groupValues?.get(1)?.lowercase()

        /**
         * refresh signal columns on an existing row (keeping its id, status, notes, and detected_at)
         * otherwise build a new `PENDING` row stamped `detectedAt`.
         */
        fun merge(
            existing: SuspiciousPackagePairCandidateEntity?,
            view: SuspiciousPackagePairView,
            derivedOwnerLogin: String?,
            detectedAt: Instant,
        ): SuspiciousPackagePairCandidateEntity =
            existing?.copy(
                versionCount = view.versionCount,
                firstReleaseTs = view.firstReleaseTs,
                lastReleaseTs = view.lastReleaseTs,
            ) ?: SuspiciousPackagePairCandidateEntity(
                projectId = view.projectId,
                artifactId = view.artifactId,
                groupId = view.groupId,
                versionCount = view.versionCount,
                firstReleaseTs = view.firstReleaseTs,
                lastReleaseTs = view.lastReleaseTs,
                derivedOwnerLogin = derivedOwnerLogin,
                status = CandidateStatus.PENDING,
                notes = null,
                detectedAt = detectedAt,
            )

        private fun keyOf(e: SuspiciousPackagePairCandidateEntity) =
            Triple(e.projectId, e.artifactId, e.groupId)

        private fun keyOf(v: SuspiciousPackagePairView) =
            Triple(v.projectId, v.artifactId, v.groupId)
    }
}
