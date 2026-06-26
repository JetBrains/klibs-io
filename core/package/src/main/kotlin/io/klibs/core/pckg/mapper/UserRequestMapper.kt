package io.klibs.core.pckg.mapper

import io.klibs.core.pckg.dto.UserIndexingRequestDto
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.enums.UserRequestProcessingStatus
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(componentModel = "spring")
abstract class UserRequestMapper {

    fun toEntity(dto: UserIndexingRequestDto): UserRequestIssueEntity {
        return UserRequestIssueEntity(
            id = null,
            githubIssueNumber = dto.githubIssueNumber,
            groupId = dto.groupId,
            artifactId = dto.artifactId,
            version = dto.version,
            processingStatus = UserRequestProcessingStatus.NEW,
            failedAttempts = 0
        )
    }
}
