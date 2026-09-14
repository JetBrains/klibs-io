package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.NonKmpPackageEntity
import org.springframework.data.repository.CrudRepository

interface NonKmpPackageRepository : CrudRepository<NonKmpPackageEntity, Long> {
}