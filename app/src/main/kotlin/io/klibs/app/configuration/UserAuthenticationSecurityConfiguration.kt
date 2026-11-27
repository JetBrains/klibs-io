package io.klibs.app.configuration

import io.klibs.app.configuration.properties.UserAuthenticationProperties
import io.klibs.app.filter.AUTHENTICATED_USER_AUTHORITY
import io.klibs.app.filter.SessionOriginProtectionFilter
import io.klibs.app.filter.UserSessionAuthenticationFilter
import io.klibs.app.service.SessionCookieService
import io.klibs.core.user.service.UserSessionService
import io.klibs.integration.hub.configuration.properties.HubAuthenticationProperties.Companion.ENABLED_PROPERTY
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.logout.LogoutFilter
import org.springframework.security.web.savedrequest.NullRequestCache
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

internal const val AUTHENTICATION_ENDPOINTS_PATH = "/auth/**"
internal const val USER_ACTIONS_PATH = "/user/**"
internal const val CURRENT_USER_PATH = "/auth/current-user"
internal const val SIGN_OUT_PATH = "/auth/sign-out"
internal const val HUB_SIGN_IN_PATH = "/auth/hub/sign-in"
internal const val HUB_CALLBACK_PATH = "/auth/hub/callback"

@Configuration
@ConditionalOnProperty(ENABLED_PROPERTY, havingValue = "true")
class UserAuthenticationSecurityConfiguration {

    @Bean
    @Order(1)
    fun authenticationFilterChain(
        http: HttpSecurity,
        authenticationCorsConfigurationSource: UrlBasedCorsConfigurationSource,
        sessionCookieService: SessionCookieService,
        userAuthenticationProperties: UserAuthenticationProperties,
        userSessionService: UserSessionService,
    ): SecurityFilterChain {
        http.configureStatelessAuthentication()

        http {
            securityMatcher(AUTHENTICATION_ENDPOINTS_PATH)

            csrf {
                disable()
            }

            cors {
                configurationSource = authenticationCorsConfigurationSource
            }

            authorizeHttpRequests {
                authorize(HttpMethod.GET, HUB_SIGN_IN_PATH, permitAll)
                authorize(HttpMethod.GET, HUB_CALLBACK_PATH, permitAll)
                authorize(HttpMethod.GET, CURRENT_USER_PATH, hasAuthority(AUTHENTICATED_USER_AUTHORITY))
                authorize(HttpMethod.POST, SIGN_OUT_PATH, permitAll)
                authorize(HttpMethod.OPTIONS, AUTHENTICATION_ENDPOINTS_PATH, permitAll)
                authorize(anyRequest, denyAll)
            }
        }

        http.addFilterBefore(
            SessionOriginProtectionFilter(
                sessionCookieService,
                userAuthenticationProperties.trustedFrontendOrigin,
                PathPatternRequestMatcher.pathPattern(HttpMethod.POST, SIGN_OUT_PATH),
            ),
            LogoutFilter::class.java,
        )
        http.addFilterBefore(
            UserSessionAuthenticationFilter(
                sessionCookieService,
                userSessionService,
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, CURRENT_USER_PATH),
            ),
            AnonymousAuthenticationFilter::class.java,
        )
        return http.build()
    }

    @Bean
    @Order(2)
    fun userActionFilterChain(
        http: HttpSecurity,
        authenticationCorsConfigurationSource: UrlBasedCorsConfigurationSource,
        sessionCookieService: SessionCookieService,
        userAuthenticationProperties: UserAuthenticationProperties,
        userSessionService: UserSessionService,
    ): SecurityFilterChain {
        http.configureStatelessAuthentication()

        http {
            securityMatcher(USER_ACTIONS_PATH)

            csrf {
                disable()
            }

            cors {
                configurationSource = authenticationCorsConfigurationSource
            }

            authorizeHttpRequests {
                authorize(HttpMethod.OPTIONS, USER_ACTIONS_PATH, permitAll)
                authorize(anyRequest, hasAuthority(AUTHENTICATED_USER_AUTHORITY))
            }
        }

        http.addFilterBefore(
            SessionOriginProtectionFilter(sessionCookieService, userAuthenticationProperties.trustedFrontendOrigin),
            LogoutFilter::class.java,
        )
        http.addFilterBefore(
            UserSessionAuthenticationFilter(sessionCookieService, userSessionService),
            AnonymousAuthenticationFilter::class.java,
        )
        return http.build()
    }

    private fun HttpSecurity.configureStatelessAuthentication() {
        this {
            requestCache {
                requestCache = NullRequestCache()
            }

            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }
        }
    }
}
