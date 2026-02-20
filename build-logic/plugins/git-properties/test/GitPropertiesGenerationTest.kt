import org.amper.gitcommitid.GitCommitIdSettings
import org.amper.gitcommitid.generateGitProperties
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertEquals

class GitPropertiesGenerationTest {

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun test() {
        val url = this.javaClass.classLoader.getResource("testRepo") ?: error("testRepo not found")
        val testRepo = url.toURI().toPath()

        val tempDir = Files.createTempDirectory("git-commit-id-amper-plugin-test-")
        val actualGitProperties = tempDir / "git.properties"

        val pluginSettings = object: GitCommitIdSettings {
            override val gitDirectory: Path
                get() = testRepo / "dotgit"
            override val abbrevLength: Int
                get() = 10
        }

        generateGitProperties(pluginSettings, actualGitProperties)

        assertTrue(actualGitProperties.exists()) {
            "git.properties file not generated"
        }
        val expectedGitProperties = Path("") / "testResources" / "expected.git.properties"
        assertEquals(expectedGitProperties.readText().sanitizeDate(), actualGitProperties.readText().sanitizeDate())
    }

    private fun String.sanitizeDate() = this
        .replace(Regex("git.build.time=.*"), "git.build.time=<TIME>")
        .replace(Regex("#.*"), "#<TIME>")
}
