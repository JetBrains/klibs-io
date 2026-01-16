package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.app.util.BackoffProvider
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import com.ninjasquad.springmockk.MockkBean
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(OutputCaptureExtension::class)
class GitHubIndexingServiceOwnerHomepageTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: GitHubIndexingService

    @Autowired
    private lateinit var scmOwnerRepository: ScmOwnerRepository

    @MockkBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockkBean(name = "ownerBackoffProvider")
    private lateinit var ownerBackoffProvider: BackoffProvider

    @Test
    @Sql("classpath:sql/GitHubIndexingServiceOwnerHomepageTest/insert-owner-incorrect-homepage.sql")
    fun `should update owner without changing homepage link`() {
        val givenHomepage = "https://voize.de"
        val expectedHomepage = "https://voize.de"
        assertOwnerHomepageSync(givenHomepage, expectedHomepage)
    }

    @Test
    @Sql("classpath:sql/GitHubIndexingServiceOwnerHomepageTest/insert-owner-incorrect-homepage.sql")
    fun `should update owner and change http to https in homepage link`() {
        val givenHomepage = "http://voize.de"
        val expectedHomepage = "https://voize.de"
        assertOwnerHomepageSync(givenHomepage, expectedHomepage)
    }

    @Test
    @Sql("classpath:sql/GitHubIndexingServiceOwnerHomepageTest/insert-owner-incorrect-homepage.sql")
    fun `should update owner and add https to homepage link`() {
        val givenHomepage = "voize.de"
        val expectedHomepage = "https://voize.de"
        assertOwnerHomepageSync(givenHomepage, expectedHomepage)
    }

    private fun assertOwnerHomepageSync(givenHomepage: String, expectedHomepage : String) {
        val login = "voize-gmbh"
        val userId = 62517686L

        val before = scmOwnerRepository.findByLogin(login)
        assertNotNull(before, "Owner entity should exist before test")

        val updatedGitHubUser = GitHubUser(
            id = userId,
            login = login,
            type = "User",
            name = "Test User Updated",
            company = "New Company",
            blog = givenHomepage,
            location = "New Location",
            email = "new@example.com",
            bio = "New bio",
            twitterUsername = "newtwitter",
            followers = 20,
        )
        every { gitHubIntegration.getUser(login) } returns updatedGitHubUser

        uut.syncOwnerWithGitHub()

        val after = scmOwnerRepository.findByLogin(login)
        assertNotNull(after, "Owner entity should exist after sync method call")

        assertEquals(expectedHomepage, after.homepage)
    }
}