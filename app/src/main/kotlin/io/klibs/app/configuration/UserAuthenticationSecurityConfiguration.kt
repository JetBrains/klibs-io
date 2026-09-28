package io.klibs.app.configuration

import io.klibs.app.auth.AUTHENTICATED_USER_AUTHORITY
import io.klibs.app.auth.SessionCookieService
import io.klibs.app.auth.SessionOriginProtectionFilter
import io.klibs.app.auth.TrustedOriginValidator
import io.klibs.app.auth.UserSessionAuthenticationFilter
import io.klibs.core.user.service.UserSessionService
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

@Configuration
@ConditionalOnProperty("klibs.auth.enabled", havingValue = "true")
class UserAuthenticationSecurityConfiguration {

    @Bean
    @Order(1)
    fun authenticationFilterChain(
        http: HttpSecurity,
        authenticationCorsConfigurationSource: UrlBasedCorsConfigurationSource,
        sessionCookieService: SessionCookieService,
        trustedOriginValidator: TrustedOriginValidator,
        userSessionService: UserSessionService,
    ): SecurityFilterChain {
        http {
            securityMatcher(AUTHENTICATION_ENDPOINTS_PATH)

            csrf {
                disable()
            }

            cors {
                configurationSource = authenticationCorsConfigurationSource
            }

            authorizeHttpRequests {
                authorize(HttpMethod.GET, CURRENT_USER_PATH, permitAll)
                authorize(HttpMethod.POST, SIGN_OUT_PATH, permitAll)
                authorize(HttpMethod.OPTIONS, AUTHENTICATION_ENDPOINTS_PATH, permitAll)
                authorize(anyRequest, denyAll)
            }
        }

        http.addFilterBefore(
            SessionOriginProtectionFilter(
                sessionCookieService,
                trustedOriginValidator,
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
        trustedOriginValidator: TrustedOriginValidator,
        userSessionService: UserSessionService,
    ): SecurityFilterChain {
        http {
            securityMatcher(USER_ACTIONS_PATH)

            csrf {
                disable()
            }

            cors {
                configurationSource = authenticationCorsConfigurationSource
            }

            requestCache {
                requestCache = NullRequestCache()
            }

            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }

            authorizeHttpRequests {
                authorize(HttpMethod.OPTIONS, USER_ACTIONS_PATH, permitAll)
                authorize(anyRequest, hasAuthority(AUTHENTICATED_USER_AUTHORITY))
            }
        }

        http.addFilterBefore(
            SessionOriginProtectionFilter(sessionCookieService, trustedOriginValidator),
            LogoutFilter::class.java,
        )
        http.addFilterBefore(
            UserSessionAuthenticationFilter(sessionCookieService, userSessionService),
            AnonymousAuthenticationFilter::class.java,
        )
        return http.build()
    }

    private companion object {
        const val CURRENT_USER_PATH = "/auth/current-user"
        const val SIGN_OUT_PATH = "/auth/sign-out"
    }
}
