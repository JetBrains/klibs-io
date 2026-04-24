package io.klibs.app.indexing

import io.klibs.integration.maven.androidx.GradleMetadata
import io.klibs.integration.maven.androidx.Variant
import io.klibs.integration.maven.androidx.VariantDependency
import io.klibs.integration.maven.androidx.VariantDependencyVersion
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AggregateVariantDependenciesTest {

    @Test
    fun `empty metadata yields empty set`() {
        val result = aggregateVariantDependencies(GradleMetadata(variants = null), "g", "a")
        assertEquals(emptySet(), result)
    }

    @Test
    fun `variants with no dependencies yield empty set`() {
        val md = GradleMetadata(
            variants = listOf(
                Variant(attributes = mapOf("org.jetbrains.kotlin.platform.type" to "jvm")),
                Variant(attributes = mapOf("org.jetbrains.kotlin.platform.type" to "js")),
            )
        )
        assertEquals(emptySet(), aggregateVariantDependencies(md, "g", "a"))
    }

    @Test
    fun `same dependency in multiple variants is deduped`() {
        val coroutines = VariantDependency(
            group = "org.jetbrains.kotlinx",
            module = "kotlinx-coroutines-core",
            version = VariantDependencyVersion(requires = "1.10.1"),
        )
        val md = GradleMetadata(
            variants = listOf(
                Variant(dependencies = listOf(coroutines)),
                Variant(dependencies = listOf(coroutines)),
                Variant(dependencies = listOf(coroutines)),
            )
        )
        val result = aggregateVariantDependencies(md, "g", "a")
        assertEquals(
            setOf(Triple("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.1")),
            result,
        )
    }

    @Test
    fun `different versions of same dependency across variants are both kept`() {
        val v1 = VariantDependency(
            group = "org.jetbrains.kotlinx",
            module = "kotlinx-coroutines-core",
            version = VariantDependencyVersion(requires = "1.9.0"),
        )
        val v2 = VariantDependency(
            group = "org.jetbrains.kotlinx",
            module = "kotlinx-coroutines-core",
            version = VariantDependencyVersion(requires = "1.10.1"),
        )
        val md = GradleMetadata(
            variants = listOf(
                Variant(dependencies = listOf(v1)),
                Variant(dependencies = listOf(v2)),
            )
        )
        val result = aggregateVariantDependencies(md, "g", "a")
        assertEquals(
            setOf(
                Triple("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.9.0"),
                Triple("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.10.1"),
            ),
            result,
        )
    }

    @Test
    fun `disjoint per-variant dependencies are all collected`() {
        val jvmDep = VariantDependency(
            group = "org.jetbrains.kotlinx",
            module = "kotlinx-coroutines-core-jvm",
            version = VariantDependencyVersion(requires = "1.10.1"),
        )
        val jsDep = VariantDependency(
            group = "org.jetbrains.kotlinx",
            module = "kotlinx-coroutines-core-js",
            version = VariantDependencyVersion(requires = "1.10.1"),
        )
        val nativeDep = VariantDependency(
            group = "org.jetbrains.kotlinx",
            module = "atomicfu",
            version = VariantDependencyVersion(requires = "0.23.0"),
        )
        val md = GradleMetadata(
            variants = listOf(
                Variant(dependencies = listOf(jvmDep)),
                Variant(dependencies = listOf(jsDep)),
                Variant(dependencies = listOf(nativeDep)),
            )
        )
        val result = aggregateVariantDependencies(md, "g", "a")
        assertEquals(
            setOf(
                Triple("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.10.1"),
                Triple("org.jetbrains.kotlinx", "kotlinx-coroutines-core-js", "1.10.1"),
                Triple("org.jetbrains.kotlinx", "atomicfu", "0.23.0"),
            ),
            result,
        )
    }

    @Test
    fun `self reference across variants is filtered out`() {
        val selfRef = VariantDependency(
            group = "io.klibs",
            module = "some-lib",
            version = VariantDependencyVersion(requires = "1.0.0"),
        )
        val realDep = VariantDependency(
            group = "org.jetbrains.kotlin",
            module = "kotlin-stdlib",
            version = VariantDependencyVersion(requires = "2.1.0"),
        )
        val md = GradleMetadata(
            variants = listOf(
                Variant(dependencies = listOf(selfRef, realDep)),
                Variant(dependencies = listOf(selfRef)),
            )
        )
        val result = aggregateVariantDependencies(md, selfGroupId = "io.klibs", selfArtifactId = "some-lib")
        assertEquals(
            setOf(Triple("org.jetbrains.kotlin", "kotlin-stdlib", "2.1.0")),
            result,
        )
    }

    @Test
    fun `dependency without requires or prefers is dropped`() {
        val good = VariantDependency(
            group = "g1",
            module = "a1",
            version = VariantDependencyVersion(requires = "1.0"),
        )
        val noVersion = VariantDependency(
            group = "g2",
            module = "a2",
            version = null,
        )
        val emptyVersion = VariantDependency(
            group = "g3",
            module = "a3",
            version = VariantDependencyVersion(requires = null, prefers = null),
        )
        val md = GradleMetadata(
            variants = listOf(Variant(dependencies = listOf(good, noVersion, emptyVersion)))
        )
        val result = aggregateVariantDependencies(md, "g", "a")
        assertEquals(setOf(Triple("g1", "a1", "1.0")), result)
    }

    @Test
    fun `prefers is used when requires is absent`() {
        val dep = VariantDependency(
            group = "g1",
            module = "a1",
            version = VariantDependencyVersion(requires = null, prefers = "2.0"),
        )
        val md = GradleMetadata(variants = listOf(Variant(dependencies = listOf(dep))))
        val result = aggregateVariantDependencies(md, "g", "a")
        assertEquals(setOf(Triple("g1", "a1", "2.0")), result)
    }

    @Test
    fun `requires takes precedence over prefers`() {
        val dep = VariantDependency(
            group = "g1",
            module = "a1",
            version = VariantDependencyVersion(requires = "1.0", prefers = "2.0"),
        )
        val md = GradleMetadata(variants = listOf(Variant(dependencies = listOf(dep))))
        val result = aggregateVariantDependencies(md, "g", "a")
        assertEquals(setOf(Triple("g1", "a1", "1.0")), result)
    }
}
