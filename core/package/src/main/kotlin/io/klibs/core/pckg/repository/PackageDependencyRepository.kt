package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.PackageDependencyEntity
import io.klibs.core.pckg.entity.PackageDependencyKey
import org.springframework.data.repository.CrudRepository

interface PackageDependencyRepository : CrudRepository<PackageDependencyEntity, PackageDependencyKey> {

    fun deleteAllByIdPackageId(packageId: Long)
}
