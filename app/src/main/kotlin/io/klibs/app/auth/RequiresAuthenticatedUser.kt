package io.klibs.app.auth

import org.springframework.security.access.prepost.PreAuthorize

const val AUTHENTICATED_USER_AUTHORITY = "KLIBS_USER"

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('KLIBS_USER')")
annotation class RequiresAuthenticatedUser
