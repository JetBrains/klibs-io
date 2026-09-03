package io.klibs.notifier

import org.gradle.api.provider.Property

interface KlibsIoNotifierExtension {
    val apiUrl: Property<String>
    val publishTaskName: Property<String>
}
