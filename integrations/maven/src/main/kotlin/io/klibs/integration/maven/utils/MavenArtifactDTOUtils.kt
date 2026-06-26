package io.klibs.integration.maven.utils

import io.klibs.integration.maven.dto.GavCoordinatesDTO

class MavenArtifactDTOUtils {
    companion object {
        /**
         * Checks if the data in MavenArtifactDTO is in valid format.
         *
         * Returns null if the request is valid, or an error message if it is not
         */
        fun validateGAVField(parsed: GavCoordinatesDTO): String? {
            // Regex for group id and artifact id: Alphanumeric characters, dots, underscores, and hyphens.
            val regex = "^[A-Za-z0-9_.-]+$".toRegex()

            // Regex for version: Forbidding control characters, and characters manipulating URL path
            val versionRegex = "^[^\\p{Cntrl}/\\\\%?#&]+$".toRegex()

            if (!parsed.groupId.matches(regex)) {
                return "Invalid Group ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed."
            }
            if (!parsed.artifactId.matches(regex)) {
                return "Invalid Artifact ID format. Only alphanumeric characters, dots, underscores, and hyphens are allowed."
            }

            if (parsed.version != null && !parsed.version.matches(versionRegex)) {
                return "Invalid Version format. Newlines, other control characters, and the following characters are not allowed: /, \\, %, ?, #, &."
            }

            return null
        }
    }
}
