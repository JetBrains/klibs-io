package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.app.job.GitHubOwnerUpdatingService
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.owner.ScmOwnerSchedulingRepository
import io.klibs.integration.github.GitHubApiException
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubUser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val SEED = "classpath:sql/GitHubOwnerRenamedOrDeletedTest/insert-owners.sql"

private const val VOIZE_LOGIN = "voize-gmbh"
private const val VOIZE_NATIVE_ID = 62517686L
private const val XING_LOGIN = "XingHeYuZhuan"

@ExtendWith(OutputCaptureExtension::class)
class GitHubOwnerRenamedOrDeletedTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var ownerUpdatingService: GitHubOwnerUpdatingService

    @Autowired
    private lateinit var scmOwnerRepository: ScmOwnerRepository

    @Autowired
    private lateinit var schedulingRepository: ScmOwnerSchedulingRepository

    @MockitoBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @Test
    @Sql(SEED)
    fun `a renamed owner keeps its row and gets the new login`() {
        deferOtherOwner(XING_LOGIN)
        val newLogin = "voize-health"
        whenever(gitHubIntegration.getUser(VOIZE_LOGIN)).thenReturn(null)
        whenever(gitHubIntegration.getUser(VOIZE_NATIVE_ID)).thenReturn(githubUser(VOIZE_NATIVE_ID, newLogin))

        ownerUpdatingService.syncOwnerWithGitHub()

        assertNull(scmOwnerRepository.findByLogin(VOIZE_LOGIN), "the old login should no longer resolve")
        val renamed = assertNotNull(scmOwnerRepository.findByLogin(newLogin))
        assertEquals(VOIZE_NATIVE_ID, renamed.nativeId, "the rename must reuse the same owner row")
        assertNull(schedulingRepository.find(renamed.idNotNull), "a resolved rename is a success, not a failure")
    }

    @Test
    @Sql(SEED)
    fun `an owner whose login was taken over by a different account is followed by native id`() {
        deferOtherOwner(XING_LOGIN)
        val newLogin = "voize-health"
        whenever(gitHubIntegration.getUser(VOIZE_LOGIN)).thenReturn(githubUser(999999L, VOIZE_LOGIN))
        whenever(gitHubIntegration.getUser(VOIZE_NATIVE_ID)).thenReturn(githubUser(VOIZE_NATIVE_ID, newLogin))

        ownerUpdatingService.syncOwnerWithGitHub()

        val followed = assertNotNull(scmOwnerRepository.findByLogin(newLogin))
        assertEquals(VOIZE_NATIVE_ID, followed.nativeId, "must follow our own native id, not the login squatter")
    }

    @Test
    @Sql(SEED)
    fun `a failing native id probe is retried, never treated as deleted`() {
        deferOtherOwner(XING_LOGIN)
        whenever(gitHubIntegration.getUser(VOIZE_LOGIN)).thenReturn(null)
        whenever(gitHubIntegration.getUser(VOIZE_NATIVE_ID))
            .thenThrow(GitHubApiException(429, "https://api.github.com/user/$VOIZE_NATIVE_ID", "rate limited"))

        ownerUpdatingService.syncOwnerWithGitHub()

        val voizeId = assertNotNull(scmOwnerRepository.findByLogin(VOIZE_LOGIN))
        val deferred = assertNotNull(schedulingRepository.find(voizeId.idNotNull))
        assertEquals(
            "GitHubApiException",
            deferred.reason.substringBefore(':'),
            "an unreachable probe is not proof of deletion"
        )
    }

    @Test
    @Sql(SEED)
    fun `a deleted owner keeps its data and stays visible`(output: CapturedOutput) {
        deferOtherOwner(XING_LOGIN)
        val before = assertNotNull(scmOwnerRepository.findByLogin(VOIZE_LOGIN))
        whenever(gitHubIntegration.getUser(any<String>())).thenReturn(null)
        whenever(gitHubIntegration.getUser(any<Long>())).thenReturn(null)

        ownerUpdatingService.syncOwnerWithGitHub()

        val after = assertNotNull(scmOwnerRepository.findByLogin(VOIZE_LOGIN), "the owner row must not be removed")
        assertEquals(before, after, "a deleted owner keeps its data untouched, including updated_at")

        val deferred = assertNotNull(schedulingRepository.find(after.idNotNull))
        assertContains(deferred.reason, "ScmOwnerDeletedException")

        assertContains(output.out, "Deferring a deleted GitHub owner")
        assertContains(output.out, VOIZE_LOGIN)
        assert(!output.out.contains("Error while updating a GitHub owner")) {
            "a deleted account is expected, so it must not reach the ERROR stream"
        }
    }

    /** Defers the owner we do not want selected, so [ScmOwnerRepository.findForUpdate] is deterministic. */
    private fun deferOtherOwner(login: String) {
        val other = assertNotNull(scmOwnerRepository.findByLogin(login))
        schedulingRepository.scheduleNextRetry(
            other.idNotNull,
            attempts = 1,
            backoffDelaySeconds = 3600,
            reason = "deferred by the test"
        )
    }

    private fun githubUser(id: Long, login: String) = GitHubUser(
        id = id,
        login = login,
        type = "Organization",
        name = "Test Owner",
        company = null,
        blog = null,
        location = null,
        email = null,
        bio = null,
        twitterUsername = null,
        followers = 1
    )
}
