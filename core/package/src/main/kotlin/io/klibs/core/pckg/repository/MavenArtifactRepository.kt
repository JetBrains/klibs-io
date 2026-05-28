package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.MavenArtifactEntity
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface MavenArtifactRepository : CrudRepository<MavenArtifactEntity, Long> {

    fun findByGroupIdAndArtifactIdAndVersion(
        groupId: String,
        artifactId: String,
        version: String,
    ): MavenArtifactEntity?

    @Modifying
    @Query(
        value = """
            INSERT INTO MavenArtifactEntity (groupId, artifactId, version)
            VALUES (:groupId, :artifactId, :version)
            ON CONFLICT (groupId, artifactId, version) DO NOTHING
        """,
    )
    fun saveIfAbsent(
        @Param("groupId") groupId: String,
        @Param("artifactId") artifactId: String,
        @Param("version") version: String,
    ): Long

    /**
     * Bulk-fetches the rows whose `(groupId, artifactId, version)` triple matches any element
     * of [keys]. Each key must be packed as `"$groupId|$artifactId|$version"`.
     */
    @Query(
        """
            SELECT a FROM MavenArtifactEntity a
            WHERE CONCAT(a.groupId, '|', a.artifactId, '|', a.version) IN :keys
        """
    )
    fun findAllByPackedKey(@Param("keys") keys: Collection<String>): List<MavenArtifactEntity>
}
