package io.klibs.core.scm.repository.readme.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class GithubThemeMarkdownReadmeProcessorTest {

    private val processor = GithubThemeMarkdownReadmeProcessor()

    @Test
    fun testProcess() {

        val readmeContent = """
            ![This link should be updated](docs/assets/readme_logo_light.png#gh-light-mode-only)
            ![This link should be deleted](docs/assets/readme_logo_dark.png#gh-dark-mode-only)
            ![This link should stay as it is](docs/assets/readme_logo_dark.png)
            [This link should stay as it is](docs/assets/readme_logo_dark.png#gh-dark-mode-only)
            [This link should stay as it is](docs/assets/readme_logo_light.png#gh-light-mode-only)

            <img src="https://raw.githubusercontent.com/GiorgosXou/Random-stuff/main/Programming/StackOverflow/Answers/70200610_11465149/b.png#gh-light-mode-only" height="120" width="120"/>
            <img src="https://raw.githubusercontent.com/GiorgosXou/Random-stuff/main/Programming/StackOverflow/Answers/70200610_11465149/w.png#gh-dark-mode-only" height="120" width="120"/>

            <a target="_blank" rel="noopener noreferrer" href="docs/assets/readme_logo_light.png#gh-light-mode-only">
            <img src="docs/assets/readme_logo_light.png#gh-light-mode-only" alt="logo" style="max-width: 100%;"> 
            </a>
            
            <a target="_blank" rel="noopener noreferrer" href="docs/assets/readme_logo_dark.png#gh-dark-mode-only"><img src="docs/assets/readme_logo_dark.png#gh-dark-mode-only" alt="logo" style="max-width: 100%;"></a>

            <a href="https://github.com/jsoizo/kotlin-csv/blob/master/LICENSE#gh-light-mode-only">Should stay the same</a>
            <a href="https://github.com/jsoizo/kotlin-csv/blob/master/LICENSE#gh-dark-mode-only">Should stay the same</a>
            
            <table>
                  <tr>
                    <td style="text-align:center;"><img src="images/first.png" alt="✓"></td>
                    <td><a href="README.md">Should stay the same</a></td>
                  </tr>
                  <tr>
                    <td style="text-align:center;"><img src="https://img.shields.io/badge/%20✓-green?style=flat-square&color=276221" alt="✓"></td>
                    <td><a target="_blank" rel="noopener noreferrer" href="docs/assets/readme_logo_light.png#gh-light-mode-only"><img src="docs/assets/readme_logo_light.png#gh-light-mode-only" alt="logo" style="max-width: 100%;"></a></td>
                  </tr>
            </table>
            """.trimIndent()

        val expectedReadmeContent =
            """
            ![This link should be updated](docs/assets/readme_logo_light.png)
            
            ![This link should stay as it is](docs/assets/readme_logo_dark.png)
            [This link should stay as it is](docs/assets/readme_logo_dark.png#gh-dark-mode-only)
            [This link should stay as it is](docs/assets/readme_logo_light.png#gh-light-mode-only)

            <img src="https://raw.githubusercontent.com/GiorgosXou/Random-stuff/main/Programming/StackOverflow/Answers/70200610_11465149/b.png" height="120" width="120"/>


            <a target="_blank" rel="noopener noreferrer" href="docs/assets/readme_logo_light.png">
            <img src="docs/assets/readme_logo_light.png" alt="logo" style="max-width: 100%;"> 
            </a>
            
            
            
            <a href="https://github.com/jsoizo/kotlin-csv/blob/master/LICENSE#gh-light-mode-only">Should stay the same</a>
            <a href="https://github.com/jsoizo/kotlin-csv/blob/master/LICENSE#gh-dark-mode-only">Should stay the same</a>
            
            <table>
                  <tr>
                    <td style="text-align:center;"><img src="images/first.png" alt="✓"></td>
                    <td><a href="README.md">Should stay the same</a></td>
                  </tr>
                  <tr>
                    <td style="text-align:center;"><img src="https://img.shields.io/badge/%20✓-green?style=flat-square&color=276221" alt="✓"></td>
                    <td><a target="_blank" rel="noopener noreferrer" href="docs/assets/readme_logo_light.png"><img src="docs/assets/readme_logo_light.png" alt="logo" style="max-width: 100%;"></a></td>
                  </tr>
            </table>
            """.trimIndent()

        val processedMarkdownReadme = processor.process(readmeContent, "Kotlin", "kotlindl", "master")

        Assertions.assertEquals(expectedReadmeContent, processedMarkdownReadme)
    }
}