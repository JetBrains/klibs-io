package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.NonKmpPackageEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface NonKmpPackageRepository : CrudRepository<NonKmpPackageEntity, Long> {

    @Modifying
    @Query(
        value = """
            INSERT INTO non_kmp_packages (maven_artifact_id)
            VALUES (:mavenArtifactId)
            ON CONFLICT (maven_artifact_id) DO NOTHING
        """,
        nativeQuery = true,
    )
    fun saveIfAbsent(@Param("mavenArtifactId") mavenArtifactId: Long): Int
}
