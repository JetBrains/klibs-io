package io.klibs.core.search

enum class SearchSort(
    val serializableName: String
) {
    MOST_STARS(serializableName = "most-stars"),
    RELEVANCY(serializableName = "relevance");

    companion object {
        fun findBySerializableName(input: String): SearchSort {
            return when (input) {
                MOST_STARS.serializableName -> MOST_STARS
                RELEVANCY.serializableName -> RELEVANCY
                else -> throw IllegalArgumentException("Unexpected sort option: $input")
            }
        }
    }
}
