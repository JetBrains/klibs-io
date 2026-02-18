package io.klibs.core.readme.impl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class LinksMarkdownReadmeProcessorTest {

    private val processor = LinksMarkdownReadmeProcessor()

    @Test
    fun testProcess() {

        val readmeContent = """
            [This link should be updated](docs/quick_start_guide.md)
            [This link should also be updated](./docs/quick_start_guide.md)
            [This link should also be updated](artwork/readme/apps.png)
            [This link should not be update](https://klibs.io/)
            [This link should also not be update](http://klibs.io/)
            <a href="https://github.com/jsoizo/kotlin-csv/blob/master/LICENSE">
              <img alt="License: Apache License 2.0" src="https://img.shields.io/badge/License-Apache License 2.0-yellow.svg" target="_blank" />
            </a>
            <table>
                  <tr>
                    <td style="text-align:center;"><img src="images/first.png" alt="✓"></td>
                    <td><a href="README.md">It is updatable link</a></td>
                  </tr>
                  <tr>
                    <td style="text-align:center;"><img src="https://img.shields.io/badge/%20✓-green?style=flat-square&color=276221" alt="✓"></td>
                    <td><a href="https://github.com/the-best-is-best/KFirebase/blob/main/FirebaseMessaging/readme.md">It is not updatable link</a></td>
                  </tr>
            </table>
            """.trimIndent()

        val expectedReadmeContent =
            """
            [This link should be updated](https://github.com/Kotlin/kotlindl/blob/master/docs/quick_start_guide.md)
            [This link should also be updated](https://github.com/Kotlin/kotlindl/blob/master/./docs/quick_start_guide.md)
            [This link should also be updated](https://raw.githubusercontent.com/Kotlin/kotlindl/master/artwork/readme/apps.png)
            [This link should not be update](https://klibs.io/)
            [This link should also not be update](http://klibs.io/)
            <a href="https://github.com/jsoizo/kotlin-csv/blob/master/LICENSE">
              <img alt="License: Apache License 2.0" src="https://img.shields.io/badge/License-Apache License 2.0-yellow.svg" target="_blank" />
            </a>
            <table>
                  <tr>
                    <td style="text-align:center;"><img src="https://raw.githubusercontent.com/Kotlin/kotlindl/master/images/first.png" alt="✓"></td>
                    <td><a href="https://github.com/Kotlin/kotlindl/blob/master/README.md">It is updatable link</a></td>
                  </tr>
                  <tr>
                    <td style="text-align:center;"><img src="https://img.shields.io/badge/%20✓-green?style=flat-square&color=276221" alt="✓"></td>
                    <td><a href="https://github.com/the-best-is-best/KFirebase/blob/main/FirebaseMessaging/readme.md">It is not updatable link</a></td>
                  </tr>
            </table>
            """.trimIndent()

        val processedMarkdownReadme = processor.process(readmeContent, "Kotlin", "kotlindl", "master")

        Assertions.assertEquals(expectedReadmeContent, processedMarkdownReadme)
    }
}
