package io.klibs.app.service.impl

import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.configuration.properties.GitHubIntegrationProperties
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*

class UserGitHubIssueNotifierTest {

    private val gitHubIntegration: GitHubIntegration = mock()
    private val gitHubIntegrationProperties: GitHubIntegrationProperties = mock()
    private val indexRequests: GitHubIntegrationProperties.IndexRequests = mock()

    private lateinit var uut: UserGitHubIssueNotifier

    @BeforeEach
    fun setUp() {
        whenever(gitHubIntegrationProperties.indexRequests).thenReturn(indexRequests)
        whenever(indexRequests.processedLabel).thenReturn("processed")
        uut = UserGitHubIssueNotifier(gitHubIntegration, gitHubIntegrationProperties)
    }

    @Test
    fun `notifies success`() {
        uut.notifyAccepted(123)

        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat { contains("accepted") })
        verify(gitHubIntegration).addKlibsIssueLabel(123, "processed")
    }

    @Test
    fun `notifies failure with reason and pings developer`() {
        whenever(indexRequests.developerHandle).thenReturn("dev")

        uut.notifyFailure(123, "some reason")

        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat {
            contains("some reason") && contains("cc @dev")
        })
        verify(gitHubIntegration).addKlibsIssueLabel(123, "processed")
    }

    @Test
    fun `notifies parse failure and pings developer`() {
        whenever(indexRequests.developerHandle).thenReturn("dev")

        uut.notifyFailure(123, null)

        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat {
            contains("Indexing request could not be processed") && contains("cc @dev")
        })
        verify(gitHubIntegration).addKlibsIssueLabel(123, "processed")
    }

    @Test
    fun `notifies indexing success and pings developer`() {
        whenever(indexRequests.developerHandle).thenReturn("dev")

        uut.notifyIndexingSuccess(123)

        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat {
            contains("Indexing completed") && contains("cc @dev")
        })
    }

    @Test
    fun `notifies developer on internal error`() {
        whenever(indexRequests.developerHandle).thenReturn("dev")

        uut.notifyServerErrorFailure(123)

        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat {
            contains("Developers have been notified") &&
                    contains("cc @dev")
        })
        verify(gitHubIntegration).addKlibsIssueLabel(123, "processed")
    }
}
