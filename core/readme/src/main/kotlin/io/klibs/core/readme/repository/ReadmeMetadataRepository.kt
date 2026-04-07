package io.klibs.core.readme.repository

import io.klibs.core.readme.entity.ReadmeMetadataEntity
import org.springframework.data.jpa.repository.JpaRepository

interface ReadmeMetadataRepository : JpaRepository<ReadmeMetadataEntity, Int>{
    fun findByScmRepoId(scmRepoId: Int): ReadmeMetadataEntity?
}