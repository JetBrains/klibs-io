package io.klibs.core.project

import io.klibs.core.project.entity.Marker
import io.klibs.core.project.enums.MarkerType
import io.klibs.core.project.repository.MarkerRepository
import io.klibs.core.project.service.OwnerMarkerService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OwnerMarkerServiceTest {

    private val markerRepository = mockk<MarkerRepository>()
    private val uut = OwnerMarkerService(markerRepository)

    @Test
    fun `JetBrains owner gets JETBRAINS_AND_GOOGLE marker`() {
        val captor = mutableListOf<Iterable<Marker>>()
        every { markerRepository.saveAll(capture(captor)) } answers { firstArg<Iterable<Marker>>().toList() }

        uut.assignOwnerMarkers(projectId = 42, ownerLogin = "JetBrains")

        verify(exactly = 1) { markerRepository.saveAll(any<Iterable<Marker>>()) }
        assertEquals(listOf(Marker(42, MarkerType.JETBRAINS_AND_GOOGLE)), captor.single().toList())
    }

    @Test
    fun `owner login is matched case-insensitively`() {
        val captor = mutableListOf<Iterable<Marker>>()
        every { markerRepository.saveAll(capture(captor)) } answers { firstArg<Iterable<Marker>>().toList() }

        uut.assignOwnerMarkers(projectId = 7, ownerLogin = "kotlin")

        assertEquals(listOf(Marker(7, MarkerType.JETBRAINS_AND_GOOGLE)), captor.single().toList())
    }

    @Test
    fun `androidx owner gets JETBRAINS_AND_GOOGLE marker`() {
        val captor = mutableListOf<Iterable<Marker>>()
        every { markerRepository.saveAll(capture(captor)) } answers { firstArg<Iterable<Marker>>().toList() }

        uut.assignOwnerMarkers(projectId = 9, ownerLogin = "androidx")

        assertEquals(listOf(Marker(9, MarkerType.JETBRAINS_AND_GOOGLE)), captor.single().toList())
    }

    @Test
    fun `other owner gets no markers`() {
        uut.assignOwnerMarkers(projectId = 1, ownerLogin = "square")

        verify(exactly = 0) { markerRepository.saveAll(any<Iterable<Marker>>()) }
    }
}
