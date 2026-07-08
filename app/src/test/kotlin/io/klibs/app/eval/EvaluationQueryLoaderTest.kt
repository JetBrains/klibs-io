package io.klibs.app.eval

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.core.io.ByteArrayResource

class EvaluationQueryLoaderTest {

    private fun loaderOf(content: String) = EvaluationQueryLoader(ByteArrayResource(content.toByteArray()))

    @Test
    fun `loads most frequent queries first and skips comments and blanks`() {
        val loader = loaderOf(
            """
            # header comment
            navigation${'\t'}6

            image picker${'\t'}2
            json${'\t'}9
            """.trimIndent()
        )

        val queries = loader.load(limit = 2)

        assertEquals(listOf("json", "navigation"), queries.map { it.text })
        assertEquals(listOf(9, 6), queries.map { it.frequency })
    }

    @Test
    fun `defaults frequency to 1 when the column is missing`() {
        val queries = loaderOf("dependency injection").load(limit = 10)
        assertEquals(1, queries.single().frequency)
        assertEquals("dependency injection", queries.single().text)
    }
}
