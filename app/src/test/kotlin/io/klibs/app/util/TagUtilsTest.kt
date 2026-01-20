package io.klibs.app.util

import io.klibs.core.project.utils.normalizeTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TagUtilsTest {

    @Test
    @DisplayName("trims, lowercases and keeps alphanumerics")
    fun basicNormalization() {
        assertEquals("hello-world-test", normalizeTag("  Hello--WORLD__test  "))
        assertEquals("abc-123", normalizeTag("ABC 123"))
    }

    @Test
    @DisplayName("collapses multiple separators to single dash and trims edges")
    fun collapseAndTrimDashes() {
        assertEquals("foo-bar", normalizeTag("foo---___---bar"))
        assertEquals("c-c", normalizeTag("C++/C#"))
        assertEquals("", normalizeTag("----"))
        assertEquals("", normalizeTag("-_-_-_"))
    }

    @Test
    @DisplayName("empty and whitespace-only inputs produce empty string")
    fun emptyInputs() {
        assertEquals("", normalizeTag(""))
        assertEquals("", normalizeTag("   \t  \n  "))
    }

    @Test
    @DisplayName("already normalized input remains unchanged")
    fun alreadyNormalized() {
        assertEquals("abc-123", normalizeTag("abc-123"))
    }

    @Test
    @DisplayName("camelCase just lowercases (no extra separators)")
    fun camelCase() {
        assertEquals("camelcase", normalizeTag("CamelCase"))
    }
}
