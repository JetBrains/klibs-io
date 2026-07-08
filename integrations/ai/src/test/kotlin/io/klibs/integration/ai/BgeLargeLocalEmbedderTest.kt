package io.klibs.integration.ai

import org.junit.jupiter.api.Test
import kotlin.math.sqrt
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BgeLargeLocalEmbedderTest {

    @Test
    fun `normalize produces a unit-length vector`() {
        val normalized = BgeLargeLocalEmbedder.normalize(floatArrayOf(3f, 4f))

        val length = sqrt(normalized.fold(0.0) { acc, v -> acc + v * v })
        assertEquals(1.0, length, 1e-6, "L2 norm should be 1 after normalization")
        assertEquals(0.6f, normalized[0], 1e-6f)
        assertEquals(0.8f, normalized[1], 1e-6f)
    }

    @Test
    fun `normalize leaves a zero vector unchanged`() {
        val zero = floatArrayOf(0f, 0f, 0f)

        val normalized = BgeLargeLocalEmbedder.normalize(zero)

        assertTrue(normalized.all { it == 0f }, "A zero vector must not divide by zero")
    }
}
