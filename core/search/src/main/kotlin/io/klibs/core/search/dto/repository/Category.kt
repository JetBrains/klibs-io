package io.klibs.core.search.dto.repository

data class Category(
    val id: Int,
    val name: String,
    val markers: List<String>,
    val subtitle: String?,
)