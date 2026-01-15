package io.klibs.app.util.instant

import java.time.Instant

interface InstantRepository {
    fun save(instant: Instant)

    fun retrieveLatest(): Instant?
}