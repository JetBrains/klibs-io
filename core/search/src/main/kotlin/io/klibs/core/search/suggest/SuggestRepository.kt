package io.klibs.core.search.suggest

interface SuggestRepository {
    fun suggestWords(
        query: String?,
        limit: Int
    ): List<String>

    fun refreshIndex()
}