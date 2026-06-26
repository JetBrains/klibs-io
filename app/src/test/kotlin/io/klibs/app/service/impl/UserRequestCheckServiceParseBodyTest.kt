package io.klibs.app.service.impl

import io.klibs.core.pckg.utils.UserIndexingRequestParser
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserRequestCheckServiceParseBodyTest {

    private fun uut() = UserIndexingRequestParser()

    private fun body(g: String?, a: String?, v: String?) = buildString {
        if (g != null) append("### Group ID\n\n$g\n\n")
        if (a != null) append("### Artifact ID\n\n$a\n\n")
        if (v != null) append("### Version\n\n$v\n\n")
    }

    @Test
    fun `should return parsed values for a well-formed body`() {
        val parsed = uut().parseBody(
            body(
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core",
                "1.10.2"
            )
        )
        assertNotNull(parsed)
        assertEquals("org.jetbrains.kotlinx", parsed.groupId)
        assertEquals("kotlinx-coroutines-core", parsed.artifactId)
        assertEquals("1.10.2", parsed.version)
    }

    @Test
    fun `should treat _No response_ as null version`() {
        val parsed =  uut().parseBody(
            body(
                "org.jetbrains.kotlinx",
                "kotlinx-coroutines-core",
                "_No response_"
            )
        )

        assertNotNull(parsed)
        assertNull(parsed.version)
    }

    @Test
    fun `should return null when required field is missing`() {
        val parsed =  uut().parseBody(
            body(
                "org.jetbrains.kotlinx",
                null,
                null
            )
        )
        assertNull(parsed)
    }

    @Test
    fun `should return null for invalid body`() {
        assertNull( uut().parseBody("This is not a body from the issue template"))
        assertNull( uut().parseBody(""))
        assertNull( uut().parseBody(null))
    }
}