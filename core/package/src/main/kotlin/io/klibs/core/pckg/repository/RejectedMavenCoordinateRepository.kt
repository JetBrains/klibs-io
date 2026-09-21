package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.RejectedMavenCoordinateEntity
import org.springframework.data.repository.CrudRepository

interface RejectedMavenCoordinateRepository : CrudRepository<RejectedMavenCoordinateEntity, Long>