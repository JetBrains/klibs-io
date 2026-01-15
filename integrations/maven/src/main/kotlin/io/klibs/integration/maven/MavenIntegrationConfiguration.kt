package io.klibs.integration.maven

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [MavenIntegrationProperties::class])
@ComponentScan(basePackages = ["io.klibs.integration.maven"])
class MavenIntegrationConfiguration
