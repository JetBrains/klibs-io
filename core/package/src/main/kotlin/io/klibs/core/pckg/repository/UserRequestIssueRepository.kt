package io.klibs.core.pckg.repository

import io.klibs.core.pckg.entity.UserRequestIssueEntity
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface UserRequestIssueRepository : CrudRepository<UserRequestIssueEntity, UUID>