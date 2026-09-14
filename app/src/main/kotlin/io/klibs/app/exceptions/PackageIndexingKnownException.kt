package io.klibs.app.exceptions

import io.klibs.core.pckg.dto.MavenCoordinatesDTO
import io.klibs.core.pckg.enums.PackageIndexingErrorType
import io.klibs.integration.maven.ScraperType
import java.time.Instant

/**
 * Exception class representing known errors encountered during the indexing of Maven packages.
 * @property errorType The type of indexing error encountered (e.g., missing tooling metadata).
 * @property coordinates maven GAV coordinates
 * @property scraperType scraperType the type of maven artifacts discoverer
 * @property scmUrl the url of scm repository for this package
 * */
class PackageIndexingKnownException(
    val errorType: PackageIndexingErrorType,
    val coordinates: MavenCoordinatesDTO,
    val scraperType: ScraperType,
    val scmUrl: String?,
) : RuntimeException()
