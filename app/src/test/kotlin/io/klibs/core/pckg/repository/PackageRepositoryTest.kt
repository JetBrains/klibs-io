package io.klibs.core.pckg.repository

import BaseUnitWithDbLayerTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ActiveProfiles("test")
class PackageRepositoryTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @Test
    @Sql("classpath:sql/PackageRepositoryTest/insert-known-non-kmp-package.sql")
    fun `should include non-KMP artifacts in known maven central packages`() {
        val knownPackages = packageRepository.findAllKnownMavenCentralPackages()
        val knownArtifact = knownPackages.find { it.groupId == "com.example" && it.artifactId == "non-kmp-artifact" }

        assertNotNull(knownArtifact)
        assertEquals(setOf("1.0.0"), knownArtifact.versions)
    }
}