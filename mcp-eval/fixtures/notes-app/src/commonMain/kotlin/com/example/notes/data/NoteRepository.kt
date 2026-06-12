package com.example.notes.data

import com.example.notes.model.Note
import kotlinx.coroutines.flow.Flow

/** Source of truth for notes. */
interface NoteRepository {
    fun observeAll(): Flow<List<Note>>
    suspend fun get(id: String): Note?
    suspend fun upsert(note: Note)
    suspend fun delete(id: String)
}
