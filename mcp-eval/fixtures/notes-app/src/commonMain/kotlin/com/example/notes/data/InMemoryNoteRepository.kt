package com.example.notes.data

import com.example.notes.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory repository. Everything lives in a [MutableStateFlow] and is gone when the process
 * dies — there is no on-disk persistence yet.
 */
class InMemoryNoteRepository : NoteRepository {
    private val notes = MutableStateFlow<List<Note>>(emptyList())

    override fun observeAll() = notes.asStateFlow()

    override suspend fun get(id: String): Note? = notes.value.firstOrNull { it.id == id }

    override suspend fun upsert(note: Note) {
        notes.value = notes.value.filterNot { it.id == note.id } + note
    }

    override suspend fun delete(id: String) {
        notes.value = notes.value.filterNot { it.id == id }
    }
}
