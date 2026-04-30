package io.klibs.core.search.controller

enum class SearchSort(
    val serializableName: String
) {
    MOST_STARS(serializableName = "most-stars"),
    MOST_DEPENDENTS(serializableName = "most-dependents"),
    MOST_HEALTHY(serializableName = "most-healthy"),
    RELEVANCY(serializableName = "relevance");

    companion object {
        fun findBySerializableName(input: String): SearchSort {
            return when (input) {
                MOST_STARS.serializableName -> MOST_STARS
                MOST_DEPENDENTS.serializableName -> MOST_DEPENDENTS
                MOST_HEALTHY.serializableName -> MOST_HEALTHY
                RELEVANCY.serializableName -> RELEVANCY
                else -> throw IllegalArgumentException("Unexpected sort option: $input")
            }
        }
    }
}