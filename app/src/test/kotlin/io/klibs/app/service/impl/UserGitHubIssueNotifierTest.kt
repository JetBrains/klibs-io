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
        uut.notifySuccess(123)

        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat { contains("accepted") })
        verify(gitHubIntegration).addKlibsIssueLabel(123, "processed")
    }

    @Test
    fun `notifies failure with reason`() {
        uut.notifyFailure(123, "some reason")

        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat { contains("some reason") })
        verify(gitHubIntegration).addKlibsIssueLabel(123, "processed")
    }

    @Test
    fun `notifies parse failure`() {
        uut.notifyParseFailure(123)

        verify(gitHubIntegration).addKlibsIssueComment(eq(123), argThat { contains("Could not read") })
        verify(gitHubIntegration).addKlibsIssueLabel(123, "processed")
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
