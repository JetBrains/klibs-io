package io.klibs.app.service.impl

import BaseUnitWithDbLayerTest
import com.ninjasquad.springmockk.MockkBean
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.service.UserIndexingRequestService
import io.klibs.app.service.UserIssueNotifier
import io.klibs.core.pckg.dto.UserIndexingRequestDto
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.enums.UserRequestProcessingStatus
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.*

class UserRequestServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: UserRequestService

    @MockkBean
    private lateinit var userRequestIssueRepository: UserRequestIssueRepository

    @MockkBean(relaxUnitFun = true)
    private lateinit var userIssueNotifier: UserIssueNotifier

    @MockkBean(relaxUnitFun = true)
    private lateinit var userIndexingRequestService: UserIndexingRequestService

    @BeforeEach
    fun setUp() {
        every { userRequestIssueRepository.save(any<UserRequestIssueEntity>()) } answers {
            val entity = firstArg<UserRequestIssueEntity>()
            if (entity.id == null) entity.copy(id = UUID.randomUUID()) else entity
        }
    }

    @Test
    fun `comments and labels as triaged on success`() {
        uut.processRequest(createMockDto(123, "g", "a", null))

        verify(exactly = 1, timeout = 1000) { userIndexingRequestService.discoverAndSaveRequest(any<UUID>()) }
        verify(exactly = 1, timeout = 1000) { userIssueNotifier.notifyAccepted(123) }

        verify(exactly = 1) { userRequestIssueRepository.save(match { it.githubIssueNumber == 123 && it.groupId == "g" && it.artifactId == "a" &&
                it.processingStatus == UserRequestProcessingStatus.NEW }) }
        verify(exactly = 1, timeout = 1000) { userRequestIssueRepository.save(match {
            it.processingStatus == UserRequestProcessingStatus.ACCEPTED
        }) }
    }

    @Test
    fun `comments with reason as triaged on UserRequestProcessingException`() {
        every { userIndexingRequestService.discoverAndSaveRequest(any<UUID>()) } throws
            UserRequestProcessingException(
                "No Kotlin Multiplatform artifacts found for g.a: ${HttpStatus.BAD_REQUEST}"
            )

        uut.processRequest(createMockDto(123, "g", "a", null))

        verify(exactly = 1, timeout = 1000) { userIndexingRequestService.discoverAndSaveRequest(any<UUID>()) }
        verify(exactly = 1, timeout = 1000) {
            userIssueNotifier.notifyFailure(
                123,
                "No Kotlin Multiplatform artifacts found for g.a: 400 BAD_REQUEST"
            )
        }
        verify(exactly = 1, timeout = 1000) { userRequestIssueRepository.save(match {
            it.processingStatus == UserRequestProcessingStatus.REJECTED
        }) }
    }

    @Test
    fun `comments validation error and labels on invalid request data`() {
        uut.processRequest(createMockDto(123, "group with spaces", "a", "1.0.0"))

        verify(exactly = 0) { userIndexingRequestService.discoverAndSaveRequest(any<UUID>()) }
        verify(exactly = 1) { userIssueNotifier.notifyFailure(123, match { it.contains("Invalid Group ID format") }) }
    }

    @Test
    fun `notifies failure on unexpected exception`() {
        every { userIndexingRequestService.discoverAndSaveRequest(any<UUID>()) } throws
            IllegalStateException("unexpected exception")

        uut.processRequest(createMockDto(123, "g", "a", null))

        verify(exactly = 1, timeout = 1000) { userIndexingRequestService.discoverAndSaveRequest(any<UUID>()) }
        verify(exactly = 1, timeout = 1000) { userIssueNotifier.notifyServerErrorFailure(123) }
        verify(exactly = 1, timeout = 1000) { userRequestIssueRepository.save(match {
            it.processingStatus == UserRequestProcessingStatus.FAILED
        }) }
    }

    @Test
    fun `notifies failure if repository save fails`() {
        every { userRequestIssueRepository.save(any()) } throws RuntimeException("DB is down")

        uut.processRequest(createMockDto(123, "g", "a", null))

        verify(exactly = 1) { userIssueNotifier.notifyServerErrorFailure(123) }
        verify(exactly = 0) { userIndexingRequestService.discoverAndSaveRequest(any<UUID>()) }
    }

    private fun createMockDto(
        number: Int = 123,
        g: String = "g",
        a: String = "a",
        v: String? = null
    ) = UserIndexingRequestDto(
        githubIssueNumber = number,
        groupId = g,
        artifactId = a,
        version = v,
        createdAt = Instant.now()
    )
}
