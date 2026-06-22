package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.UserRequestReportEntity
import org.springframework.data.repository.CrudRepository

interface UserRequestReportRepository : CrudRepository<UserRequestReportEntity, Int>