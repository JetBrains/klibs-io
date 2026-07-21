package io.klibs.app.dto

import io.klibs.core.pckg.enums.UserRequestIndexingStatus
import java.util.UUID

data class UserRequestReport(
    val reportId: Long,
    val userRequestIssueId: UUID,
    val groupId: String,
    val artifactId: String,
    val version: String,
    val indexingStatus: UserRequestIndexingStatus,
    val statusDetails: String?,
)
