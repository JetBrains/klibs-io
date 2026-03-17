package io.klibs.core.project.dto

data class AllowedTag(
    val name: String,
    val definition: String?,
    val positiveCues: List<String> = emptyList(),
    val negativesCues: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
)