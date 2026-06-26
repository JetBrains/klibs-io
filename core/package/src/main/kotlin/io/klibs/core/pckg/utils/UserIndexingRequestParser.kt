package io.klibs.core.pckg.utils

import io.klibs.integration.maven.dto.GavCoordinatesDTO
import org.springframework.stereotype.Component

@Component
class UserIndexingRequestParser {

    /**
     * Extracts groupId, artifactId, and version from the Markdown issue body.
     */
    fun parseBody(body: String?): GavCoordinatesDTO? {
        if (body.isNullOrBlank()) return null

        val groupId = extractField(body, "Group ID") ?: return null
        val artifactId = extractField(body, "Artifact ID") ?: return null
        val version = extractField(body, "Version")

        return GavCoordinatesDTO(groupId, artifactId, version)
    }

    private fun extractField(body: String, label: String): String? {
        val text = body.replace("\r\n", "\n")
        val header = "### $label\n"

        if (!text.contains(header)) return null

        val raw = text
            .substringAfter(header)
            .substringBefore("\n### ")
            .trim()

        if (raw.isBlank() || raw.equals("_No response_", ignoreCase = true)) return null
        return raw
    }
}
