package io.klibs.app.service.impl

import io.klibs.app.service.UserIndexingRequestService
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.service.UserIssueNotifier
import io.klibs.app.service.UserRequestService
import io.klibs.core.pckg.dto.UserIndexingRequestDto
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.enums.UserRequestProcessingStatus
import io.klibs.core.pckg.mapper.UserRequestMapper
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.integration.maven.dto.GavCoordinatesDTO
import io.klibs.integration.maven.utils.MavenArtifactDTOUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Processes a single GitHub user-submitted package indexing request delivered via
 * webhook: validates the Maven coordinates, runs indexing and reports the outcome
 * back to the issue.
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
            if (!isUserIndexingRequestValid(userIndexingRequestDto)) return

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

    private fun isUserIndexingRequestValid(userIndexingRequestDto: UserIndexingRequestDto): Boolean {
        val requestValidationError = MavenArtifactDTOUtils.validateGAVField(
            GavCoordinatesDTO(
                userIndexingRequestDto.groupId,
                userIndexingRequestDto.artifactId,
                userIndexingRequestDto.version
            )
        )

        if (requestValidationError != null) {
            userIssueNotifier.notifyFailure(userIndexingRequestDto.githubIssueNumber, requestValidationError)
            return false
        }
        return true
    }

    /**
     * Runs the indexing call for an already-validated request and posts the outcome
     * back to the issue.
     */
    private fun processValidRequest(savedIssueRequest: UserRequestIssueEntity) {
        val issueNumber = savedIssueRequest.githubIssueNumber
        try {
            userIndexingRequestService.fulfillRequest(requireNotNull(savedIssueRequest.id))
            updateProcessingStatus(savedIssueRequest, UserRequestProcessingStatus.ACCEPTED)
            userIssueNotifier.notifyAccepted(issueNumber)
        } catch (e: UserRequestProcessingException) {
            updateProcessingStatus(savedIssueRequest, UserRequestProcessingStatus.REJECTED)
            userIssueNotifier.notifyFailure(issueNumber, e.reason)
        } catch (e: Exception) {
            logger.error("Background processing failed for issue #${issueNumber}", e)
            updateProcessingStatus(savedIssueRequest, UserRequestProcessingStatus.FAILED)
            userIssueNotifier.notifyServerErrorFailure(issueNumber)
        }
    }

    private fun updateProcessingStatus(issue: UserRequestIssueEntity, status: UserRequestProcessingStatus) {
        userRequestIssueRepository.save(issue.copy(processingStatus = status))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultUserRequestService::class.java)
    }
}

