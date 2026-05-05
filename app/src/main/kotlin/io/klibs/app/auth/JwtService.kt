package io.klibs.app.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import io.klibs.app.configuration.properties.AuthProperties
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class JwtService(authProperties: AuthProperties) {

    private val algorithm = Algorithm.HMAC256(authProperties.jwt.secret)
    private val verifier = JWT.require(algorithm).withIssuer(ISSUER).build()

    fun issue(githubLogin: String): String = JWT.create()
        .withIssuer(ISSUER)
        .withSubject(githubLogin)
        .withExpiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
        .sign(algorithm)

    fun extractLogin(token: String): String? = try {
        verifier.verify(token).subject
    } catch (_: JWTVerificationException) {
        null
    }

    companion object {
        private const val ISSUER = "klibs.io"
    }
}
