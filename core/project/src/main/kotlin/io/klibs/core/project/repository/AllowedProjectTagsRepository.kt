package io.klibs.core.project.repository

import io.klibs.core.project.entity.AllowedTagEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AllowedProjectTagsRepository : JpaRepository<AllowedTagEntity, String>