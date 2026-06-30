package io.klibs.core.pckg.mapper

import io.klibs.core.pckg.dto.UserIndexingRequestDto
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.enums.UserRequestProcessingStatus
import org.springframework.stereotype.Component

@Component
class UserRequestMapper {

    fun toEntity(dto: UserIndexingRequestDto): UserRequestIssueEntity {
        return UserRequestIssueEntity(
            id = null,
            githubIssueNumber = dto.githubIssueNumber,
            groupId = dto.groupId,
            artifactId = dto.artifactId,
            version = dto.version,
            processingStatus = UserRequestProcessingStatus.NEW,
        )
    }
}
