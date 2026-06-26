package io.klibs.app.service.impl

import BaseUnitWithDbLayerTest
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.service.UserIssueNotifier
import io.klibs.core.pckg.dto.UserIndexingRequestDto
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.util.*

class UserRequestServiceTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: DefaultUserRequestService

    @MockitoBean
    private lateinit var userRequestIssueRepository: UserRequestIssueRepository

    @MockitoBean
    private lateinit var userIssueNotifier: UserIssueNotifier

    @MockitoBean
    private lateinit var userIndexingRequestService: DefaultUserIndexingRequestService

    @BeforeEach
    fun setUp() {
        whenever(userRequestIssueRepository.save(any<UserRequestIssueEntity>())).thenAnswer {
            val entity = it.arguments[0] as UserRequestIssueEntity
            if (entity.id == null) entity.copy(id = UUID.randomUUID()) else entity
        }
    }

    @Test
    fun `comments and labels as triaged on success`() {
        uut.processRequest(createMockDto(123, "g", "a", null))

        verify(userIndexingRequestService, timeout(1000)).fulfillRequest(any<UUID>())
        verify(userIssueNotifier, timeout(1000)).notifySuccess(123)

        verify(userRequestIssueRepository).save(argThat {
            githubIssueNumber == 123 && groupId == "g" && artifactId == "a"
        })
    }

    @Test
    fun `comments with reason as triaged on UserRequestProcessingException`() {
        whenever(userIndexingRequestService.fulfillRequest(any<UUID>()))
            .thenThrow(
                UserRequestProcessingException(
                    "No Kotlin Multiplatform artifacts found for g.a: ${HttpStatus.BAD_REQUEST}"
                )
            )

        uut.processRequest(createMockDto(123, "g", "a", null))

        verify(userIndexingRequestService, timeout(1000)).fulfillRequest(any<UUID>())
        verify(userIssueNotifier, timeout(1000)).notifyFailure(
            123,
            "No Kotlin Multiplatform artifacts found for g.a: 400 BAD_REQUEST"
        )
    }

    @Test
    fun `comments validation error and labels on invalid request data`() {
        uut.processRequest(createMockDto(123, "group with spaces", "a", "1.0.0"))

        verify(userIndexingRequestService, never()).fulfillRequest(any<UUID>())
        verify(userIndexingRequestService, never()).fulfillRequest(any(), any(), any())
        verify(userIssueNotifier).notifyFailure(eq(123), argThat { contains("Invalid Group ID format") })
    }

    @Test
    fun `notifies failure on unexpected exception`() {
        whenever(userIndexingRequestService.fulfillRequest(any<UUID>()))
            .thenThrow(IllegalStateException("unexpected exception"))

        uut.processRequest(createMockDto(123, "g", "a", null))

        verify(userIndexingRequestService, timeout(1000)).fulfillRequest(any<UUID>())
        verify(userIssueNotifier, timeout(1000)).notifyServerErrorFailure(eq(123))
    }

    @Test
    fun `notifies failure if repository save fails`() {
        whenever(userRequestIssueRepository.save(any()))
            .thenThrow(RuntimeException("DB is down"))

        uut.processRequest(createMockDto(123, "g", "a", null))

        verify(userIssueNotifier).notifyServerErrorFailure(eq(123))
        verify(userIndexingRequestService, never()).fulfillRequest(any<UUID>())
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
