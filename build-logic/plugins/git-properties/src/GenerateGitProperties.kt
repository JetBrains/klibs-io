package org.amper.gitcommitid

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.net.InetAddress
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream

@TaskAction(
    ExecutionAvoidance.Disabled
)
fun generateGitProperties(
    @Input settings: GitCommitIdSettings,
    @Output propertiesFile: Path,
) {
    val (gitDir, worktree) = if (settings.gitDirectory == null) {
        val gitRoot = findGitRoot() ?: error("No git repository found")
        gitRoot / ".git" to gitRoot
    } else {
        settings.gitDirectory!! to settings.gitDirectory!!.parent
    }

    val gitInfo = collectGitInfo(gitDir, worktree, settings.abbrevLength)
    val head = gitInfo.head

    val time = ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    val hostname: String = InetAddress.getLocalHost().hostName

    val properties = Properties().apply {
        setProperty("git.branch", gitInfo.currentBranch.orEmpty())
        setProperty("git.build.host", hostname)
        setProperty("git.build.time", time)

        setProperty("git.build.user.email", gitInfo.userEmail.orEmpty())
        setProperty("git.build.user.name", gitInfo.userName.orEmpty())

        // TODO no access to module settings here
        // setProperty("git.build.version", module.settings.publishing.version)

        setProperty("git.closest.tag.commit.count", gitInfo.describeResult?.commitCount.orEmpty())
        setProperty("git.closest.tag.name", gitInfo.describeResult?.tagName.orEmpty())

        setProperty("git.commit.author.time", head.authorIdent.timeAsString())
        setProperty("git.commit.committer.time", head.committerIdent.timeAsString())
        setProperty("git.commit.id", head.name())
        setProperty("git.commit.id.abbrev", head.abbreviate(settings.abbrevLength).name())

        setProperty("git.commit.id.describe", gitInfo.describeResult?.fullDescribeOutput.orEmpty())
        setProperty("git.commit.id.describe-short", gitInfo.describeResult?.shortDescribeOutput.orEmpty())

        setProperty("git.commit.message.full", head.fullMessage.trim())
        setProperty("git.commit.message.short", head.shortMessage.trim())

        setProperty("git.commit.time", head.committerIdent.timeAsString())
        setProperty("git.commit.user.email", head.committerIdent.emailAddress)
        setProperty("git.commit.user.name", head.committerIdent.name)

        setProperty("git.dirty", gitInfo.isDirty.toString())

        // todo requires fetching
        // setProperty("git.local.branch.ahead", gitInfo.localBranchAhead)
        // setProperty("git.local.branch.behind", gitInfo.localBranchBehind)

        setProperty("git.remote.origin.url", gitInfo.remoteOriginUrl.orEmpty())

        setProperty("git.tag", gitInfo.tagsOnHead.joinToString(","))
        setProperty("git.tags", gitInfo.tagsContainingHead.joinToString(","))

        setProperty("git.total.commit.count", gitInfo.totalCommitsInHead.toString())
    }

    propertiesFile.createParentDirectories().outputStream().buffered().use { out ->
        // this adds a comment at the top of the file: the date of the file generation
        // if we want to get rid of it, we should either write ourselves
        // (need to escape stuff if needed, and sort the properties beforehand),
        // or remove the line after writing
        properties.store(out, null)
    }
}

fun findGitRoot(): Path? {
    // todo need to start from the module root, not the working dir
    var currentDir = Path.of(System.getProperty("user.dir"))
    while (true) {
        val gitDir = currentDir.resolve(".git")
        // todo handle worktrees
        if (gitDir.isDirectory()) {
            return currentDir
        }
        currentDir = currentDir.parent
    }
}

private fun PersonIdent.timeAsString(): String =
    whenAsInstant.atZone(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

private data class GitInfo(
    val currentBranch: String?,
    val userName: String?,
    val userEmail: String?,
    val remoteOriginUrl: String?,
    val head: RevCommit,
    val describeResult: DescribeResult?,
    val isDirty: Boolean,
    val tagsOnHead: List<String>,
    val tagsContainingHead: List<String>,
    val totalCommitsInHead: Int
)

private fun collectGitInfo(gitDir: Path, worktreeDir: Path, abbrevLength: Int): GitInfo {
    val repo = RepositoryBuilder()
        .setWorkTree(worktreeDir.toFile())
        .setGitDir(gitDir.toFile())
        .build()
    val git = Git(repo)
    val currentBranch: String? = repo.getBranch()

    val config = repo.config
    val userName = config.getString("user", null, "name")
    val userEmail = config.getString("user", null, "email")
    val remoteOriginUrl = config.getString("remote", "origin", "url")

    // we assume that there is at least one commit in the repo
    val headCommit = git.log().setMaxCount(1).call().first()

    val describeResult = git.describeInfo(abbrevLength)

    val status = git.status().call()
    val isDirty = !(status.isClean)

    val (tagsOnHead, tagsContainingHead) = tagsOnHeadAndContainingHead(git, headCommit)
    val totalCommitsInHead = calcTotalCommitsInHead(git, headCommit)

    return GitInfo(
        currentBranch,
        userName,
        userEmail,
        remoteOriginUrl,
        headCommit,
        describeResult,
        isDirty,
        tagsOnHead,
        tagsContainingHead,
        totalCommitsInHead
    )
}

fun calcTotalCommitsInHead(git: Git, headCommit: RevCommit): Int {
    return RevWalk(git.repository).use { walk ->
        walk.markStart(walk.parseCommit(headCommit.id))
        var count = 0
        for (commit in walk) {
            count++
        }
        count
    }
}

fun tagsOnHeadAndContainingHead(git: Git, headCommit: RevCommit): Pair<List<String>, List<String>> {
    val repo = git.repository
    val tagsOnHead = mutableListOf<String>()
    val tagsContainingHead = mutableListOf<String>()
    RevWalk(repo).use { walk ->
        git.tagList().call().mapNotNull { tagRef ->
            // Peel annotated tags to the commit they point to
            val peeledRef = repo.refDatabase.peel(tagRef)
            val tagId = peeledRef.peeledObjectId ?: peeledRef.objectId
            val tagName = tagRef.name.removePrefix("refs/tags/")

            if (tagId == headCommit.id) {
                // tagRef.name is like "refs/tags/v1.2.3"
                tagsOnHead += tagName
            }

            val tagCommit = walk.parseCommit(tagId)
            // Does the tag's commit contain (is it a descendant of)
            if (walk.isMergedInto(tagCommit, headCommit)) {
                tagsContainingHead += tagName
            }
        }
    }
    return tagsOnHead to tagsContainingHead
}

private fun Git.describeInfo(abbrevLength: Int): DescribeResult? {
    val fullDescribeOutput = describe()
        .setLong(true)
        .setTags(true)
        .setAlways (true)
        .setAbbrev(abbrevLength)
        .call()
    // If describe == "v1.2.3"  -> nearest tag is "v1.2.3"
    // If describe == "v1.2.3-5-gabc1234" -> nearest tag is "v1.2.3"
    val regex = Regex("^(?<tag>.*)-(?<number>\\d+)-g(?<hash>[0-9a-fA-F]+)$")
    val match = regex.matchEntire(fullDescribeOutput) ?: return null
    val tag = match.groups["tag"]?.value ?: return null
    val number = match.groups["number"]?.value ?: return null

    val shortDescribeOutput = describe()
        .setLong(false)
        .setTags(true)
        .setAlways(true)
        .setAbbrev(abbrevLength)
        .call()
    return DescribeResult(tag, number, fullDescribeOutput, shortDescribeOutput)
}

private data class DescribeResult(
    val tagName: String,
    val commitCount: String,
    val fullDescribeOutput: String,
    val shortDescribeOutput: String,
)
