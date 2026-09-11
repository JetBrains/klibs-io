package io.klibs.notifier

import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * Allows sharing the state between the publishing tasks and [KlibsIoNotifierTask] when the configuration cache is enabled.
 */
internal abstract class KlibsIoPublishTracker : BuildService<BuildServiceParameters.None> {

    @Volatile
    var published: Boolean = false
        private set

    fun markPublished() {
        published = true
    }
}
