package io.klibs.integration.maven.androidx

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.serialization.XML

object GoogleMavenMasterIndexMetadataXmlDeserializer : DeserializationStrategy<GoogleMavenMasterIndexMetadata> {
    override val descriptor: SerialDescriptor = GoogleMavenMasterIndexMetadata.serializer().descriptor

    override fun deserialize(decoder: Decoder): GoogleMavenMasterIndexMetadata {
        val xmlDecoder = decoder as? XML.XmlInput
            ?: return decoder.decodeSerializableValue(GoogleMavenMasterIndexMetadata.serializer())

        val reader = xmlDecoder.input
        val elements = buildList {
            // Skip to first element
            var event = reader.eventType
            while (event != EventType.START_ELEMENT && event != EventType.END_DOCUMENT) {
                event = reader.next()
            }

            // Process elements
            while (event != EventType.END_DOCUMENT) {
                if (event == EventType.START_ELEMENT) {
                    val name = reader.localName
                    if (name != "metadata") {
                        val versions = reader.getAttributeValue(null, "versions")
                        add(GoogleMavenMasterIndexMetadata.Element(name, versions?.split(",") ?: emptyList()))
                    }
                }
                event = reader.next()
            }
        }
        return GoogleMavenMasterIndexMetadata(elements)
    }
}
