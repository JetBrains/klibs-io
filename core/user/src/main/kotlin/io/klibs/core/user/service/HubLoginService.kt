package io.klibs.core.user.service

import io.klibs.core.user.dto.CreatedSessionDTO
import io.klibs.core.user.model.ExternalUserIdentity
import io.klibs.integration.hub.configuration.HUB_AUTHENTICATION_ENABLED_PROPERTY
import io.klibs.integration.hub.service.HubAuthenticationService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(HUB_AUTHENTICATION_ENABLED_PROPERTY, havingValue = "true")
class HubLoginService(
    private val hubAuthenticationService: HubAuthenticationService,
    private val userService: UserService,
    private val userSessionService: UserSessionService,
) {
    fun login(code: String, codeVerifier: String): CreatedSessionDTO {
        val identity = ExternalUserIdentity(hubAuthenticationService.authenticate(code, codeVerifier))
        // Call Hub first, then handle the user and session in separate transactions.
        val user = userService.findOrCreate(identity)
        return userSessionService.createSession(user)
    }
}
