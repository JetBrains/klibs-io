package io.klibs.app.service.impl

import io.klibs.app.service.UserIndexingRequestService
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.service.UserIssueNotifier
import io.klibs.app.service.UserRequestService
import io.klibs.core.pckg.dto.UserIndexingRequestDto
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.mapper.UserRequestMapper
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.integration.maven.dto.GavCoordinatesDTO
import io.klibs.integration.maven.utils.MavenArtifactDTOUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service responsible for processing a single GitHub user-submitted package
 * indexing request, typically delivered via a GitHub webhook.
 *
 * Validates the extracted Maven coordinates, runs the indexing flow and posts
 * a result comment together with the "processed" label back to the issue.
 *
 * @property userIssueNotifier Service to notify users about the status of their request.
 * @property userIndexingRequestService Service to execute the actual package indexing.
 * @property userRequestMapper Mapper to convert between DTOs and entities.
 * @property userRequestIssueRepository Repository to persist user request issues.
 */
@Service
internal class DefaultUserRequestService(
    private val userIssueNotifier: UserIssueNotifier,
    private val userIndexingRequestService: UserIndexingRequestService,
    private val userRequestMapper: UserRequestMapper,
    private val userRequestIssueRepository: UserRequestIssueRepository,
    private val applicationScope: CoroutineScope,
) : UserRequestService {

    /**
     * Processes a single GitHub indexing-request issue end-to-end.
     *
     * Posts the appropriate status comment (success / user-side failure)
     * and applies the processed label.
     */
    override fun processRequest(userIndexingRequestDto: UserIndexingRequestDto) {
        try {
            if (validateUserIndexingRequest(userIndexingRequestDto)) return

            val savedRequest = userRequestIssueRepository.save(userRequestMapper.toEntity(userIndexingRequestDto))

            applicationScope.launch {
                processValidRequest(savedRequest)
            }
        } catch (e: UserRequestProcessingException) {
            userIssueNotifier.notifyFailure(userIndexingRequestDto.githubIssueNumber, e.reason)
        } catch (e: Exception) {
            logger.error("Initial processing failed for issue #${userIndexingRequestDto.githubIssueNumber}", e)
            userIssueNotifier.notifyServerErrorFailure(userIndexingRequestDto.githubIssueNumber)
        }
    }

    private fun validateUserIndexingRequest(userIndexingRequestDto: UserIndexingRequestDto): Boolean {
        val requestValidationError = MavenArtifactDTOUtils.validateGAVField(
            GavCoordinatesDTO(
                userIndexingRequestDto.groupId,
                userIndexingRequestDto.artifactId,
                userIndexingRequestDto.version
            )
        )

        if (requestValidationError != null) {
            userIssueNotifier.notifyFailure(userIndexingRequestDto.githubIssueNumber, requestValidationError)
            return true
        }
        return false
    }


    /**
     * Runs the indexing call for an already-validated request and posts the outcome
     * back to the issue.
     */
    private fun processValidRequest(savedIssueRequest: UserRequestIssueEntity) {
        val issueNumber = savedIssueRequest.githubIssueNumber
        try {
            userIndexingRequestService.fulfillRequest(requireNotNull(savedIssueRequest.id))
            userIssueNotifier.notifySuccess(issueNumber)
        } catch (e: UserRequestProcessingException) {
            userIssueNotifier.notifyFailure(issueNumber, e.reason)
        } catch (e: Exception) {
            logger.error("Background processing failed for issue #${issueNumber}", e)
            userIssueNotifier.notifyServerErrorFailure(issueNumber)
        }
    }


    companion object {
        private val logger = LoggerFactory.getLogger(DefaultUserRequestService::class.java)
    }
}

