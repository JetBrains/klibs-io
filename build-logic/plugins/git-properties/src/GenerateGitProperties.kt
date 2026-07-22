package org.kotlintoolchain.plugin.gitcommitid

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.jetbrains.amper.plugins.ExecutionAvoidance
import org.jetbrains.amper.plugins.Input
import org.jetbrains.amper.plugins.Output
import org.jetbrains.amper.plugins.TaskAction
import java.io.StringWriter
import java.net.InetAddress
import java.nio.file.Path
import java.time.format.DateTimeFormatter
import java.util.Properties
import kotlin.io.path.createParentDirectories
import kotlin.io.path.outputStream

@TaskAction(executionAvoidance = ExecutionAvoidance.Disabled)
fun generateGitProperties(
    @Input moduleRootDir: Path,
    @Input settings: GitCommitIdSettings,
    @Output propertiesFile: Path,
) {
    val gitInfo = openRepository(moduleRootDir, settings.gitDirectory).use { repository ->
        collectGitInfo(repository, settings.abbrevLength)
    }
    val head = gitInfo.head

    val properties = Properties().apply {
        setProperty("git.branch", gitInfo.currentBranch)
        setProperty("git.build.host", InetAddress.getLocalHost().hostName)
        setProperty("git.build.user.email", gitInfo.userEmail.orEmpty())
        setProperty("git.build.user.name", gitInfo.userName.orEmpty())
        setProperty("git.build.version", settings.buildVersion)
        setProperty("git.closest.tag.commit.count", gitInfo.describeResult.commitCount.orEmpty())
        setProperty("git.closest.tag.name", gitInfo.describeResult.tagName.orEmpty())
        setProperty("git.commit.id", head.name())
        setProperty("git.commit.id.abbrev", head.abbreviate(settings.abbrevLength).name())
        setProperty("git.commit.id.describe", gitInfo.describeResult.fullDescribeOutput)
        setProperty("git.commit.message.full", head.fullMessage.withTrailingNewline())
        setProperty("git.commit.message.short", head.shortMessage)
        setProperty("git.commit.time", head.committerIdent.timeAsString())
        setProperty("git.commit.user.email", head.committerIdent.emailAddress)
        setProperty("git.commit.user.name", head.committerIdent.name)
        setProperty("git.dirty", gitInfo.isDirty.toString())
        setProperty("git.remote.origin.url", gitInfo.remoteOriginUrl.orEmpty())
        setProperty("git.tags", gitInfo.tagsOnHead.joinToString(","))
        setProperty("git.total.commit.count", gitInfo.totalCommitsInHead.toString())
    }

    // Properties.store() always prepends a "#<Date>" comment line; strip it so the
    // output is byte-identical across builds at the same commit.
    val buffer = StringWriter()
    properties.store(buffer, null)
    val deterministic = buffer.toString().lineSequence()
        .dropWhile { it.startsWith("#") }
        .filter { it.isNotEmpty() }
        .joinToString(separator = "\n", postfix = "\n")
    propertiesFile.createParentDirectories()
    propertiesFile.outputStream().use { it.write(deterministic.toByteArray()) }
}

private fun openRepository(moduleRootDir: Path, configuredGitDirectory: Path?): Repository {
    if (configuredGitDirectory != null) {
        return FileRepositoryBuilder()
            .setGitDir(configuredGitDirectory.toFile())
            .setWorkTree(configuredGitDirectory.parent.toFile())
            .setMustExist(true)
            .build()
    }

    val repositoryBuilder = FileRepositoryBuilder()
        .readEnvironment()
        .findGitDir(moduleRootDir.toFile())
    check(repositoryBuilder.gitDir != null) {
        "No git repository found from module root '$moduleRootDir'"
    }
    return repositoryBuilder
        .setMustExist(true)
        .build()
}

private val gitTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ")

private fun PersonIdent.timeAsString(): String = whenAsInstant.atZone(zoneId).format(gitTimeFormatter)

private fun String.withTrailingNewline(): String = if (endsWith('\n')) this else "$this\n"

private data class GitInfo(
    val currentBranch: String,
    val userName: String?,
    val userEmail: String?,
    val remoteOriginUrl: String?,
    val head: RevCommit,
    val describeResult: DescribeResult,
    val isDirty: Boolean,
    val tagsOnHead: List<String>,
    val totalCommitsInHead: Int,
)

private fun collectGitInfo(repository: Repository, abbrevLength: Int): GitInfo {
    val git = Git(repository)
    val config = repository.config
    val headCommit = git.log().setMaxCount(1).call().first()
    val isDirty = !git.status().call().isClean

    return GitInfo(
        currentBranch = repository.currentBranch(),
        userName = config.getString("user", null, "name"),
        userEmail = config.getString("user", null, "email"),
        remoteOriginUrl = config.getString("remote", "origin", "url"),
        head = headCommit,
        describeResult = git.describeInfo(abbrevLength, isDirty),
        isDirty = isDirty,
        tagsOnHead = tagsOnHead(git, headCommit),
        totalCommitsInHead = calcTotalCommitsInHead(git, headCommit),
    )
}

private fun Repository.currentBranch(): String {
    val head = exactRef(Constants.HEAD) ?: return ""
    return if (head.isSymbolic) Repository.shortenRefName(head.target.name) else Constants.HEAD
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

private fun tagsOnHead(git: Git, headCommit: RevCommit): List<String> {
    val repository = git.repository
    return git.tagList().call().mapNotNull { tagRef ->
        val peeledRef = repository.refDatabase.peel(tagRef)
        val tagId = peeledRef.peeledObjectId ?: peeledRef.objectId
        tagRef.name.removePrefix(Constants.R_TAGS).takeIf { tagId == headCommit.id }
    }.sorted()
}

private fun Git.describeInfo(abbrevLength: Int, isDirty: Boolean): DescribeResult {
    val fullDescribeOutput = describe()
        .setLong(true)
        .setAlways(true)
        .setAbbrev(abbrevLength)
        .call()
    val regex = Regex("^(?<tag>.*)-(?<number>\\d+)-g(?<hash>[0-9a-fA-F]+)$")
    val match = regex.matchEntire(fullDescribeOutput)
    val dirtySuffix = if (isDirty) "-dirty" else ""

    return DescribeResult(
        tagName = match?.groups?.get("tag")?.value,
        commitCount = match?.groups?.get("number")?.value,
        fullDescribeOutput = fullDescribeOutput + dirtySuffix,
    )
}

private data class DescribeResult(
    val tagName: String?,
    val commitCount: String?,
    val fullDescribeOutput: String,
)
