package io.klibs.core.owner

enum class ScmOwnerType(
    /**
     * Name used for serialization, be it DB or other documents.
     * Should be careful with changing the name of this property
     * to avoid deserialization errors and breaking backward compatibility
     */
    val serializableName: String
) {
    ORGANIZATION("organization"),
    AUTHOR("author");

    companion object {
        fun findBySerializableName(input: String): ScmOwnerType {
            return when(input) {
                ORGANIZATION.serializableName -> ORGANIZATION
                AUTHOR.serializableName -> AUTHOR
                else -> throw IllegalArgumentException("Unknown scm owner type: $input")
            }
        }
    }
}
