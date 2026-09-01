package io.klibs.notifier

import org.gradle.api.provider.Property

interface KlibsIoNotifierExtension {
    val apiBaseUrl: Property<String>
}
