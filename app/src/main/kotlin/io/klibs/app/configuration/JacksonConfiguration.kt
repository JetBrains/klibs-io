package io.klibs.app.configuration

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JacksonConfiguration {

    @Bean
    fun kotlinModule(): Module {
        return KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    }

    @Bean
    @Primary
    fun objectMapper(builder: Jackson2ObjectMapperBuilder): ObjectMapper {
        return builder.createXmlMapper(false).build()
    }

    @Bean
    fun xmlMapper(builder: Jackson2ObjectMapperBuilder): XmlMapper {
        return builder.createXmlMapper(true).build()
    }
}
