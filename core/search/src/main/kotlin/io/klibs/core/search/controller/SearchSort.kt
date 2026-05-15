package io.klibs.core.search.controller

enum class SearchSort(
    val serializableName: String
) {
    MOST_STARS(serializableName = "most-stars"),
    MOST_DEPENDENTS(serializableName = "most-dependents"),
    RELEVANCY(serializableName = "relevance");

    companion object {
        fun findBySerializableName(input: String): SearchSort {
            return when (input) {
                MOST_STARS.serializableName -> MOST_STARS
                MOST_DEPENDENTS.serializableName -> MOST_DEPENDENTS
                RELEVANCY.serializableName -> RELEVANCY
                else -> throw IllegalArgumentException("Unexpected sort option: $input")
            }
        }
    }
}