import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.kotlintoolchain.plugin.gitcommitid.GitCommitIdSettings
import org.kotlintoolchain.plugin.gitcommitid.generateGitProperties
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.readText
import kotlin.io.path.toPath
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class GitPropertiesGenerationTest {

    @Test
    fun `generates properties compatible with the Gradle plugin`() {
        val testRepoFixture = testRepositoryPath()
        val tempDir = Files.createTempDirectory("git-properties-test-")
        val testRepo = tempDir / "test-repo"
        testRepoFixture.toFile().copyRecursively(testRepo.toFile())
        makeHeadTagAnnotated(testRepo, "t3")
        val actualGitProperties = tempDir / "git.properties"

        val pluginSettings = object : GitCommitIdSettings {
            override val gitDirectory: Path = testRepo / "dotgit"
            override val buildVersion: String = "1.2.3"
            override val abbrevLength: Int = 10
        }

        generateGitProperties(testRepo, pluginSettings, actualGitProperties)

        assertTrue(actualGitProperties.exists()) {
            "git.properties file not generated"
        }
        val expectedGitProperties = Path.of("testResources", "expected.git.properties")
        assertEquals(
            expectedGitProperties.readText().sanitizeGeneratedValues(),
            actualGitProperties.readText().sanitizeGeneratedValues(),
        )
    }

    @Test
    fun `discovers repository from a linked worktree git file`() {
        val testRepo = testRepositoryPath()
        val tempDir = Files.createTempDirectory("git-properties-worktree-test-")
        val worktree = tempDir / "worktree"
        val gitDirectory = tempDir / "git-directory"
        Files.createDirectories(worktree)
        testRepo.resolve("dotgit").toFile().copyRecursively(gitDirectory.toFile())
        Files.copy(testRepo.resolve("a.txt"), worktree.resolve("a.txt"))
        worktree.resolve(".git").writeText("gitdir: ${gitDirectory.toAbsolutePath()}\n")

        val pluginSettings = object : GitCommitIdSettings {
            override val gitDirectory: Path? = null
            override val abbrevLength: Int = 10
        }
        val actualGitProperties = tempDir / "git.properties"

        generateGitProperties(worktree, pluginSettings, actualGitProperties)

        val properties = Properties().apply {
            actualGitProperties.inputStream().use(::load)
        }
        assertEquals("master", properties.getProperty("git.branch"))
        assertEquals("b99cb369d4c788d0fa572573e8a3d34c1f76327f", properties.getProperty("git.commit.id"))
        assertEquals("t3", properties.getProperty("git.tags"))
    }

    private fun makeHeadTagAnnotated(testRepo: Path, tagName: String) {
        FileRepositoryBuilder()
            .setGitDir(testRepo.resolve("dotgit").toFile())
            .setWorkTree(testRepo.toFile())
            .build()
            .use { repository ->
                val git = Git(repository)
                git.tagDelete().setTags(tagName).call()
                val head = RevWalk(repository).use { walk ->
                    walk.parseCommit(repository.resolve(Constants.HEAD))
                }
                git.tag()
                    .setName(tagName)
                    .setObjectId(head)
                    .setMessage(tagName)
                    .call()
            }
    }

    private fun testRepositoryPath(): Path {
        val url = this.javaClass.classLoader.getResource("testRepo") ?: error("testRepo not found")
        return url.toURI().toPath()
    }

    private fun String.sanitizeGeneratedValues(): String =
        replace(Regex("git.build.host=.*"), "git.build.host=<HOST>")
}
