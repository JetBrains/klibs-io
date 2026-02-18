package io.klibs.core.readme

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ReadmeConfigurationProperties::class)
class ReadmeConfiguration
