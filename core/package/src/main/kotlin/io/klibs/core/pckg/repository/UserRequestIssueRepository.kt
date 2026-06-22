package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.UserRequestIssueEntity
import org.springframework.data.repository.CrudRepository

interface UserRequestIssueRepository : CrudRepository<UserRequestIssueEntity, Int>