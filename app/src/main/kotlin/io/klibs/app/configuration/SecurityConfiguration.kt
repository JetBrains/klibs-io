package io.klibs.app.configuration

import io.klibs.app.configuration.properties.AuthProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfiguration(
    private val environment: Environment
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf {
                disable()
            }

            cors {
                disable()
            }

            httpBasic { }

            authorizeHttpRequests {
                authorize(HttpMethod.GET, "/categories.json", permitAll)

                authorize(HttpMethod.GET, "/tags/**", permitAll)
                authorize(HttpMethod.OPTIONS, "/tags/**", permitAll)

                authorize(HttpMethod.GET, "/owner/**", permitAll)
                authorize(HttpMethod.OPTIONS, "/owner/**", permitAll)

                authorize(HttpMethod.GET, "/package/**", permitAll)
                authorize(HttpMethod.OPTIONS, "/package/**", permitAll)

                authorize(HttpMethod.GET, "/project/**", permitAll)
                authorize(HttpMethod.OPTIONS, "/project/**", permitAll)

                authorize(HttpMethod.GET, "/search/**", permitAll)
                authorize(HttpMethod.POST, "/search/**", permitAll)
                authorize(HttpMethod.OPTIONS, "/search/**", permitAll)

                authorize(HttpMethod.GET, "/ping", permitAll)
                authorize(HttpMethod.OPTIONS, "/ping", permitAll)

                authorize("/error", permitAll)

                if (environment.matchesProfiles("prod")) {
                    authorize("/blacklist/**", hasRole("ADMIN"))
                    authorize("/actuator/**", hasRole("actuator"))
                    authorize("/api-docs/**", hasRole("api-docs"))
                    authorize("/package-description/**", hasRole("ADMIN"))
                    authorize(HttpMethod.PATCH, "/content/**", hasRole("content-manager"))
                } else {
                    authorize("/blacklist/**", permitAll)
                    authorize("/actuator/**", permitAll)
                    authorize("/api-docs/**", permitAll)
                    authorize(HttpMethod.PATCH, "/content/**", permitAll)
                    authorize("/package-description/**", permitAll)
                }

                authorize(anyRequest, authenticated)
            }
        }
        return http.build()
    }

    @Bean
    fun users(adminAuthProperties: AuthProperties): UserDetailsService {
        val users = adminAuthProperties.users.map {
            User.builder()
                .username(it.username)
                .password(it.password)
                .roles(*it.roles.toTypedArray())
                .build()
        }
        return InMemoryUserDetailsManager(users)
    }
}
