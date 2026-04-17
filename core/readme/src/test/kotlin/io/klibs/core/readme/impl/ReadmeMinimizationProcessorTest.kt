package io.klibs.core.readme.impl

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ReadmeMinimizationProcessorTest {
    private val readmeMinimizationProcessor = ReadmeMinimizationProcessor()

    @Test
    fun checkMinimisation() {
        val markdownContentWithAllConstructs =
            """
            # Markdown Example

            ## Headers

            # H1 Header
            ## H2 Header
            ### H3 Header
            #### H4 Header
            ##### H5 Header
            ###### H6 Header

            ## Emphasis

            *Italic text* or _Italic text_

            **Bold text** or __Bold text__

            ***Bold and Italic text***

            ~~Strikethrough~~

            ## Lists

            ### Unordered List

            - Item 1
            - Item 2
              - Subitem 2.1
              - Subitem 2.2
            - Item 3

            ### Ordered List

            1. First item
            2. Second item
               1. Subitem 2.1
               2. Subitem 2.2
            3. Third item

            ## Links

            [OpenAI](https://openai.com)
            [Auto Link](auto/link/to.png)

            ## Images

            ![Alt text](https://via.placeholder.com/150)

            ## Code

            ### Inline Code

            Use `console.log('Hello, world!');` for logging.

            ### Code Block

            ```python
            print("Hello, world!")
            ```

            ```javascript
            console.log("Hello, world!");
            ```

            ## Blockquotes

            > This is a blockquote.
            >
            > This is another line of the blockquote.

            ## Tables

            | Column 1 | Column 2 | Column 3 |
            |----------|----------|----------|
            | Data 1   | Data 2   | Data 3   |
            | Data 4   | Data 5   | Data 6   |

            ## Horizontal Rule

            ---

            ## Task Lists

            - [x] Task 1
            - [ ] Task 2
            - [ ] Task 3

            ## Footnotes

            This is an example of a footnote reference[^1].

            [^1]: This is the footnote explanation.

            ## HTML inside Markdown

            <div style="color: blue; font-weight: bold;">This is blue text using HTML.</div>

            ## Very long text to exceed the maximum length of the minimized text.
            John Wick, the man of focus, commitment, and pure will, stands as the epitome of resilience.
            His story begins with tragedy—a world filled with loss as he attempts to move on from his past.
            Here we exceed 1000 symbols.
            """.trimIndent()

        val process = readmeMinimizationProcessor.process(markdownContentWithAllConstructs, "", "", "")
        assertEquals(
            "Markdown Example Headers H1 Header H2 Header H3 Header H4 Header H5 Header H6 Header Emphasis Italic text or Italic text Bold text or Bold text Bold and Italic text Strikethrough Lists Unordered List Item 1 Item 2 Subitem 2.1 Subitem 2.2 Item 3 Ordered List First item Second item Subitem 2.1 Subitem 2.2 Third item Links OpenAI Auto Link Images Code Inline Code Use console.log Hello, world ; for logging. Code Block Blockquotes This is a blockquote. This is another line of the blockquote. Tables Column 1 Column 2 Column 3 Data 1 Data 2 Data 3 Data 4 Data 5 Data 6 Horizontal Rule Task Lists Task 1 Task 2 Task 3 Footnotes This is an example of a footnote reference ^1 . ^1 This is the footnote explanation. HTML inside Markdown Very long text to exceed the maximum length of the minimized text. John Wick, the man of focus, commitment, and pure will, stands as the epitome of resilience. His story begins with tragedy—a world filled with loss as he attempts to move on from his past. Here we exce",
            process
        )
    }

    @Test
    fun `test basic text processing`() {
        val markdown = """
            This is a simple text.
            It should remain unchanged.
        """.trimIndent()

        val result = readmeMinimizationProcessor.process(markdown, "owner", "repo", "main")
        assertEquals("This is a simple text. It should remain unchanged.", result)
    }

    @Test
    fun `test link processing - should keep link text but remove URL`() {
        val markdown = """
            This is a [link title](https://example.com) in text.
            Another [important link](https://test.com) here.
        """.trimIndent()

        val result = readmeMinimizationProcessor.process(markdown, "owner", "repo", "main")
        assertEquals("This is a link title in text. Another important link here.", result)
    }

    @Test
    fun `test image removal`() {
        val markdown = """
            Some text without image.

            Here is some text with ![Image alt text](image.jpg) embedded image.

            More text in a separate paragraph.
        """.trimIndent()

        val result = readmeMinimizationProcessor.process(markdown, "owner", "repo", "main")
        assertEquals("Some text without image. Here is some text with embedded image. More text in a separate paragraph.", result)
    }

    @Test
    fun `test code block removal`() {
        val markdown = """
            Here is some text.
            ```kotlin
            fun test() {
                println("Hello")
            }
            ```
            More text here.
        """.trimIndent()

        val result = readmeMinimizationProcessor.process(markdown, "owner", "repo", "main")
        assertEquals("Here is some text. More text here.", result)
    }

    @Test
    fun `test complex markdown with multiple elements`() {
        val markdown = """
            # Project Title

            This is a description with a [link](https://example.com) and some **bold** text.

            ## Features
            * Feature 1
            * Feature 2

            Here's an image: ![Test Image](test.jpg)

            ```kotlin
            fun code() {
                // This should be removed
            }
            ```

            Final paragraph with [another link](https://test.com).
        """.trimIndent()

        val result = readmeMinimizationProcessor.process(markdown, "owner", "repo", "main")
        val expected = "Project Title This is a description with a link and some bold text. Features Feature 1 Feature 2 Here s an image Final paragraph with another link ."
        assertEquals(expected, result)
    }
}
