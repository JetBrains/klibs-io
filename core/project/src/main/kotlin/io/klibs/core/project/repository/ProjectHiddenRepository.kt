package io.klibs.core.project.repository

import io.klibs.core.project.entity.ProjectHiddenEntity
import io.klibs.core.project.enums.HideOrigin
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface ProjectHiddenRepository : JpaRepository<ProjectHiddenEntity, Int> {

    /**
     * Hides a project, keeping an existing row untouched so a repeated hide does not move `hidden_at`
     * or overwrite the origin of a manual hide.
     *
     * @return number of rows inserted by this call
     */
    @Modifying
    @Transactional
    @Query("""
        INSERT INTO ProjectHiddenEntity (projectId, hiddenAt, origin, reason)
        VALUES (:projectId, :hiddenAt, :origin, :reason)
        ON CONFLICT(projectId) DO NOTHING
    """)
    fun hide(
        @Param("projectId") projectId: Int,
        @Param("hiddenAt") hiddenAt: Instant,
        @Param("origin") origin: HideOrigin,
        @Param("reason") reason: String?,
    ): Int

    fun findByProjectId(projectId: Int): ProjectHiddenEntity?

    @Transactional
    fun deleteByProjectId(projectId: Int): Int

    @Transactional
    fun deleteByProjectIdInAndOrigin(projectIds: Collection<Int>, origin: HideOrigin): Int
}
