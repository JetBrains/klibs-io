package io.klibs.integration.maven.configuration

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.klibs.integration.maven.MavenIntegrationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [MavenIntegrationProperties::class])
@ComponentScan(basePackages = ["io.klibs.integration.maven"])
class MavenIntegrationConfiguration {

    @Bean
    fun xmlMapper(): XmlMapper = XmlMapper().apply {
        registerKotlinModule()
    }
}