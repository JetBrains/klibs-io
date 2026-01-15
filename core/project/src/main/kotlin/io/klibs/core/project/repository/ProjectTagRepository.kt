package io.klibs.core.project.repository

import io.klibs.core.project.entity.TagEntity
import io.klibs.core.project.entity.TagKey
import io.klibs.core.project.enums.TagOrigin
import org.springframework.data.repository.CrudRepository

interface ProjectTagRepository : CrudRepository<TagEntity, TagKey> {
    fun deleteByProjectIdAndOrigin(projectId: Int, origin: TagOrigin): Long

    fun findAllByProjectIdAndOrigin(projectId: Int, origin: TagOrigin): List<TagEntity>
}