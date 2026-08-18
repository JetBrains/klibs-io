package io.klibs.app.dto

data class PluginRequestNotification(
    val groupId: String,
    val artifactId: String,
    val version: String,
)