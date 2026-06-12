package com.example.notes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notes.model.Note
import com.mikepenz.markdown.m3.Markdown

@Composable
fun NoteDetailScreen(note: Note, onSave: (Note) -> Unit, onShare: (Note) -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var body by remember { mutableStateOf(note.body) }
    var isEditing by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
        if (isEditing) {
            TextField(value = title, onValueChange = { title = it })
            TextField(value = body, onValueChange = { body = it })
            Button(onClick = {
                onSave(note.copy(title = title, body = body))
                isEditing = false
            }) { Text("Save") }
        } else {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Markdown(body)
            Button(onClick = { isEditing = true }) { Text("Edit") }
        }
        // Sharing currently just copies text. Product wants the user to be able to share a note
        // by showing something another phone can scan from the screen.
        Button(onClick = { onShare(note) }) { Text("Share") }
    }
}
