package io.klibs.integration.maven

import org.apache.maven.search.api.MAVEN
import org.apache.maven.search.api.Record
import org.apache.maven.search.backend.smo.SmoSearchResponse
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Utility class for creating mock objects for Maven tests.
 */
object MavenTestUtils {

    /**
     * Creates a mock Record with the specified properties.
     *
     * @param groupId The group ID of the artifact
     * @param artifactId The artifact ID
     * @param version The version of the artifact
     * @param lastUpdated The timestamp when the artifact was last updated
     * @return A mock Record with the specified properties
     */
    fun createMockRecord(groupId: String, artifactId: String, version: String, lastUpdated: Long): Record {
        val record = mock<Record>()
        whenever(record.getValue(MAVEN.GROUP_ID)).thenReturn(groupId)
        whenever(record.getValue(MAVEN.ARTIFACT_ID)).thenReturn(artifactId)
        whenever(record.getValue(MAVEN.VERSION)).thenReturn(version)
        whenever(record.lastUpdated).thenReturn(lastUpdated)
        return record
    }

    /**
     * Creates a mock SmoSearchResponse with the specified properties.
     *
     * @param records The list of records to include in the response
     * @param totalHits The total number of hits for the search
     * @param currentHits The number of hits in the current page
     * @return A mock SmoSearchResponse with the specified properties
     */
    fun createMockResponse(records: List<Record>, totalHits: Int, currentHits: Int): SmoSearchResponse {
        val response = mock<SmoSearchResponse>()
        whenever(response.page).thenReturn(records)
        whenever(response.totalHits).thenReturn(totalHits)
        whenever(response.currentHits).thenReturn(currentHits)
        return response
    }
}