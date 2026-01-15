package io.klibs.core.pckg.model

enum class PackagePlatform(
    /**
     * Name used for serialization, be it DB, DTO or other documents.
     * Should be careful with changing the name of this property
     * to avoid deserialization errors and breaking backward compatibility.
     */
    val serializableName: String
) {
    COMMON(serializableName = "common"),

    JVM(serializableName = "jvm"),

    ANDROIDJVM(serializableName = "androidJvm"),

    NATIVE(serializableName = "native"),

    WASM(serializableName = "wasm"),

    JS(serializableName = "js");

    companion object {
        private val bySerializableName by lazy { entries.associateBy { it.serializableName } }

        fun findBySerializableName(filterValue: String): PackagePlatform {
            return bySerializableName[filterValue] ?: throw IllegalArgumentException("Invalid platform: $filterValue")
        }
    }
}
