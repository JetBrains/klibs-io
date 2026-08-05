package io.klibs.core.search.opensearch

import java.time.Duration

data class LockSpec(val name: String, val atMostFor: Duration, val atLeastFor: Duration)

fun interface SearchIndexLock {
    fun runLocked(spec: LockSpec, block: () -> Unit): Boolean
}
