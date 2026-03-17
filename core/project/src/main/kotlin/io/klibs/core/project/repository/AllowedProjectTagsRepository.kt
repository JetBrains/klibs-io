package io.klibs.core.project.repository

import io.klibs.core.project.entity.AllowedTagEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AllowedProjectTagsRepository : JpaRepository<AllowedTagEntity, String> {
    @Query(
        value = """
            SELECT name
            FROM allowed_project_tags apt
            WHERE name = :value
               OR EXISTS (
                SELECT 1
                FROM jsonb_array_elements_text(apt.synonyms) AS synonyms(synonym)
                WHERE lower(replace(synonym, ' ', '-')) = lower(replace(:value, ' ', '-'))
            )
            LIMIT 1;
        """,
        nativeQuery = true
    )
    fun findCanonicalNameByValue(@Param("value") value: String): String?
}