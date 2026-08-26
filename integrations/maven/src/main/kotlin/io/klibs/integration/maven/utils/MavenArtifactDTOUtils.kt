package io.klibs.integration.maven.utils

import io.klibs.integration.maven.dto.GavCoordinatesDTO

class MavenArtifactDTOUtils {
    companion object {
        // Regex for group id and artifact id: Alphanumeric characters, dots, underscores, and hyphens.
        private val GROUP_ID_AND_ARTIFACT_ID_REGEX = "^[A-Za-z0-9_.-]+$".toRegex()

        // Regex for version: Forbidding control characters, and characters manipulating URL path
        private val VERSION_REGEX = "^[^\\p{Cntrl}\\s/\\\\%?#&]+$".toRegex()

        /**
         * Checks if the data in MavenArtifactDTO is in valid format.
         *
         * Returns null if the request is valid, or an error message if it is not
         */
        fun validateGAVField(parsed: GavCoordinatesDTO): String? {

            if (!parsed.groupId.matches(GROUP_ID_AND_ARTIFACT_ID_REGEX)) {
                return "Invalid Group ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed."
            }
            if (!parsed.artifactId.matches(GROUP_ID_AND_ARTIFACT_ID_REGEX)) {
                return "Invalid Artifact ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed."
            }

            if (parsed.version != null && !parsed.version.matches(VERSION_REGEX)) {
                return "Invalid Version format. Whitespace, control characters, and the following characters are not allowed: /, \\, %, ?, #, &."
            }

            return null
        }
    }
}
