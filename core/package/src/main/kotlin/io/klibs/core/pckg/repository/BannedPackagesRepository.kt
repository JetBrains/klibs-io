package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.BannedPackagesEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository


interface BannedPackagesRepository : CrudRepository<BannedPackagesEntity, Long> {
    @Query(value = """
        SELECT EXISTS (
              SELECT 1
              FROM banned_packages bp
              WHERE bp.group_id = :groupId
                AND (bp.artifact_id = :artifactId OR bp.artifact_id IS NULL)
        )
    """, nativeQuery = true)
    fun isBanned(groupId: String, artifactId: String): Boolean
}
