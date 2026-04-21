package io.klibs.core.pckg.enums

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VersionTypeTest {

    @Test
    fun `stable release version`() {
        assertEquals(VersionType.STABLE, VersionType.from("1.0.0"))
    }

    @Test
    fun `stable release with higher numbers`() {
        assertEquals(VersionType.STABLE, VersionType.from("2.3.14"))
    }

    @Test
    fun `pre-release alpha version`() {
        assertEquals(VersionType.NON_STABLE, VersionType.from("1.0.0-alpha"))
    }

    @Test
    fun `pre-release beta version`() {
        assertEquals(VersionType.NON_STABLE, VersionType.from("1.0.0-beta.1"))
    }

    @Test
    fun `pre-release rc version`() {
        assertEquals(VersionType.NON_STABLE, VersionType.from("2.0.0-rc.1"))
    }

    @Test
    fun `pre-release snapshot version`() {
        assertEquals(VersionType.NON_STABLE, VersionType.from("1.0.0-SNAPSHOT"))
    }

    @Test
    fun `zero major version is non-stable`() {
        assertEquals(VersionType.NON_STABLE, VersionType.from("0.1.0"))
    }

    @Test
    fun `stable version with build metadata`() {
        assertEquals(VersionType.STABLE, VersionType.from("1.0.0+build.123"))
    }

    @Test
    fun `non-semver version string`() {
        assertEquals(VersionType.NON_SEMVER, VersionType.from("not-a-version"))
    }

    @Test
    fun `non-semver empty string`() {
        assertEquals(VersionType.NON_SEMVER, VersionType.from(""))
    }

    @Test
    fun `loose mode parses single number as non-stable`() {
        // In loose mode, "1" is parsed as "1.0.0" which is stable
        assertEquals(VersionType.STABLE, VersionType.from("1"))
    }

    @Test
    fun `non-semver date-based version`() {
        assertEquals(VersionType.NON_SEMVER, VersionType.from("2024.01.15"))
    }
}
