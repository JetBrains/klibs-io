package io.klibs.core.search.dto.repository

data class Category(
    val name: String,
    val subtitle: String?,
    val markers: List<String>,
)