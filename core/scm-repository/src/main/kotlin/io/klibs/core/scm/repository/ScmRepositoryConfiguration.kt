package io.klibs.core.scm.repository

import io.klibs.core.scm.repository.readme.ReadmeConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(value = [ReadmeConfigurationProperties::class])
class ScmRepositoryConfiguration
