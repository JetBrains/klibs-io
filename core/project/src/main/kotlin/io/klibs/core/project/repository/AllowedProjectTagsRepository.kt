package io.klibs.core.project.repository

import io.klibs.core.project.entity.AllowedProjectTagEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AllowedProjectTagsRepository : JpaRepository<AllowedProjectTagEntity, String> {
    @Query(
        value = """
            SELECT name
            FROM allowed_project_tags
            WHERE name = :value
               OR tag_synonyms @> to_jsonb(ARRAY[:value])
            LIMIT 1
        """,
        nativeQuery = true
    )
    fun findCanonicalNameByValue(@Param("value") value: String): String?
}