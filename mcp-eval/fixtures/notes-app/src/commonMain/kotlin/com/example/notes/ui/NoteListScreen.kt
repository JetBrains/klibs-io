package com.example.notes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notes.data.NoteRepository
import com.mikepenz.markdown.m3.Markdown

@Composable
fun NoteListScreen(repository: NoteRepository, onOpen: (String) -> Unit) {
    val notes by repository.observeAll().collectAsState(initial = emptyList())
    LazyColumn {
        items(notes) { note ->
            Column(Modifier.padding(16.dp)) {
                Text(note.title, style = MaterialTheme.typography.titleMedium)
                Markdown(note.body)
            }
        }
    }
}
