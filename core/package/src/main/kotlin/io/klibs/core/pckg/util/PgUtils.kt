package io.klibs.core.pckg.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.postgresql.util.PGobject
import java.sql.ResultSet

private val objectMapper = ObjectMapper().apply {
    registerModules(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, true)
            .configure(KotlinFeature.NullToEmptyMap, true)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, false)
            .configure(KotlinFeature.StrictNullChecks, true)
            .build()
    )
    setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
}

internal fun <T> T.toJsonPGObject(): PGobject {
    val pgObject = PGobject()
    pgObject.type = "jsonb"
    pgObject.value = objectMapper.writeValueAsString(this)
    return pgObject
}

internal inline fun <reified T> ResultSet.getJsonValue(columnName: String): T? {
    val pgObject = this.getObject(columnName) as? PGobject ?: return null
    val json = pgObject.value ?: return null
    return objectMapper.readValue(json)
}

