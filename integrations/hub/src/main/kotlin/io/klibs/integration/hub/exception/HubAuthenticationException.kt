package io.klibs.integration.hub.exception

import io.klibs.integration.hub.enums.HubAuthenticationFailure

/** Carries a safe category only; upstream messages can contain codes, tokens, or user details. */
class HubAuthenticationException(val failure: HubAuthenticationFailure) : RuntimeException(failure.name)
