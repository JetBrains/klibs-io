package io.klibs.integration.hub.configuration

import java.time.Duration

const val HUB_AUTHENTICATION_ENABLED_PROPERTY = "klibs.auth.hub.enabled"

/** Hub client inputs shared with the application without depending on its configuration classes. */
class HubClientSettings(
    val baseUrl: String,
    val clientId: String,
    val callbackUrl: String,
    val scope: String,
    val requestTimeout: Duration,
    val callbackTimeout: Duration,
)
