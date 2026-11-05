package io.klibs.app.service.impl

import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.configuration.properties.GitHubIntegrationProperties
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UserGitHubIssueNotifierTest {

    private val gitHubIntegration = mockk<GitHubIntegration>(relaxUnitFun = true)
    private val gitHubIntegrationProperties = mockk<GitHubIntegrationProperties>()
    private val indexRequests = mockk<GitHubIntegrationProperties.IndexRequests>()

    private lateinit var uut: UserGitHubIssueNotifier

    @BeforeEach
    fun setUp() {
        every { gitHubIntegrationProperties.indexRequests } returns indexRequests
        every { indexRequests.processedLabel } returns "processed"
        uut = UserGitHubIssueNotifier(gitHubIntegration, gitHubIntegrationProperties)
    }

    @Test
    fun `notifies success`() {
        uut.notifyAccepted(123)

        verify(exactly = 1) { gitHubIntegration.addKlibsIssueComment(123, match { it.contains("accepted") }) }
        verify(exactly = 1) { gitHubIntegration.addKlibsIssueLabel(123, "processed") }
    }

    @Test
    fun `notifies failure with reason and pings developer`() {
        every { indexRequests.developerHandle } returns "dev"

        uut.notifyFailure(123, "some reason")

        verify(exactly = 1) {
            gitHubIntegration.addKlibsIssueComment(
                123,
                match { it.contains("some reason") && it.contains("cc @dev") }
            )
        }
        verify(exactly = 1) { gitHubIntegration.addKlibsIssueLabel(123, "processed") }
    }

    @Test
    fun `notifies parse failure and pings developer`() {
        every { indexRequests.developerHandle } returns "dev"

        uut.notifyFailure(123, null)

        verify(exactly = 1) {
            gitHubIntegration.addKlibsIssueComment(
                123,
                match { it.contains("Indexing request could not be processed") && it.contains("cc @dev") }
            )
        }
        verify(exactly = 1) { gitHubIntegration.addKlibsIssueLabel(123, "processed") }
    }

    @Test
    fun `notifies indexing success and pings developer`() {
        every { indexRequests.developerHandle } returns "dev"

        uut.notifyIndexingSuccess(123)

        verify(exactly = 1) {
            gitHubIntegration.addKlibsIssueComment(
                123,
                match { it.contains("Indexing completed") && it.contains("cc @dev") }
            )
        }
    }

    @Test
    fun `notifies developer on internal error`() {
        every { indexRequests.developerHandle } returns "dev"

        uut.notifyServerErrorFailure(123)

        verify(exactly = 1) {
            gitHubIntegration.addKlibsIssueComment(
                123,
                match {
                    it.contains("Developers have been notified") &&
                            it.contains("cc @dev")
                }
            )
        }
        verify(exactly = 1) { gitHubIntegration.addKlibsIssueLabel(123, "processed") }
    }
}
