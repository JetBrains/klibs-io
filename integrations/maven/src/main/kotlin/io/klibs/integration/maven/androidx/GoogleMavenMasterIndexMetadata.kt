package io.klibs.integration.maven.androidx

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XML

@Serializable
data class GoogleMavenMasterIndexMetadata(val elements: List<Element>) {
    @Serializable
    data class Element(
        val name: String,
        val versions: List<String> = emptyList()
    )

    companion object {
        fun fromXml(string: String, xml: XML = XML.Companion.defaultInstance): GoogleMavenMasterIndexMetadata =
            xml.decodeFromString(deserializer = GoogleMavenMasterIndexMetadataXmlDeserializer, string = string)
    }
}