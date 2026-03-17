package io.klibs.core.project.dto

data class AllowedTag(
    val name: String,
    val definition: String?,
    val positiveCues: List<String>,
    val negativesCues: List<String>,
    val synonyms: List<String>,
)