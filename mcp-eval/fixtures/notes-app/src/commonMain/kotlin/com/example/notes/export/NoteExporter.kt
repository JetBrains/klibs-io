package com.example.notes.export

import com.example.notes.model.Note

/**
 * Exports a note so the user can keep or share it outside the app.
 *
 * TODO: not implemented yet. Product wants users to be able to save a note as a self-contained,
 * printable document they can open anywhere.
 */
interface NoteExporter {
    suspend fun export(note: Note): ByteArray
}

class StubNoteExporter : NoteExporter {
    override suspend fun export(note: Note): ByteArray =
        throw NotImplementedError("Export is not implemented yet")
}
