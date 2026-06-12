package com.example.notes.model

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val body: String,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val updatedAt: Long,
)
