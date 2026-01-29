package io.klibs.integration.maven.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "metadata")
data class MavenMetadata(
    val groupId: String = "",
    val artifactId: String = "",
    val versioning: Versioning = Versioning()
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Versioning(
        val latest: String? = null,
        val release: String? = null,
        @JacksonXmlElementWrapper(localName = "versions")
        @param:JacksonXmlProperty(localName = "versions")
        @get:JacksonXmlProperty(localName = "version")
        val versions: List<String> = emptyList(),
        val lastUpdated: String? = null
    )
}