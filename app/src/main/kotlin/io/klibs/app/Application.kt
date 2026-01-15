package io.klibs.app

import io.klibs.app.configuration.properties.GoogleMavenCacheConfigurationProperties
import io.klibs.app.configuration.properties.ApiDocsProperties
import io.klibs.app.configuration.properties.AuthProperties
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

fun main() {
    SpringApplication.run(Application::class.java)
}

@EnableConfigurationProperties(value = [AuthProperties::class, ApiDocsProperties::class, GoogleMavenCacheConfigurationProperties::class])
@SpringBootApplication(scanBasePackages = ["io.klibs"])
@EntityScan(basePackages = ["io.klibs.**.entity"])
@EnableJpaRepositories(basePackages = ["io.klibs.**.repository"])
class Application
