package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.PackageIndexEntity
import io.klibs.core.pckg.entity.PackageIndexKey
import org.springframework.data.repository.CrudRepository

interface PackageIndexRepository : CrudRepository<PackageIndexEntity, PackageIndexKey> {
    fun findByIdGroupId(groupId: String): List<PackageIndexEntity>

    fun findByProjectId(projectId: Int): List<PackageIndexEntity>
}