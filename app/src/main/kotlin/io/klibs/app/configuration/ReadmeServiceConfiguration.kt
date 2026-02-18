package io.klibs.app.configuration

import io.klibs.core.readme.ReadmeConfigurationProperties
import io.klibs.core.readme.service.S3ReadmeService
import io.klibs.core.storage.S3StorageService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ReadmeServiceConfiguration {

    @Bean
    fun s3ReadmeService(
        readmeProperties: ReadmeConfigurationProperties,
        s3StorageService: S3StorageService,
    ): S3ReadmeService = S3ReadmeService(readmeProperties, s3StorageService)
}
