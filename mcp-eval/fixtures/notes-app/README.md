# Notes (Kotlin Multiplatform)

A small cross-platform notes app — **Android, iOS, Desktop (JVM)** — built with Compose
Multiplatform.

## What works today
- Create / edit / delete notes (`model/Note.kt`, `data/NoteRepository.kt`).
- In-memory storage only (`data/InMemoryNoteRepository.kt`) — **notes are lost on restart**.
- A list screen and a detail/edit screen (`ui/NoteListScreen.kt`, `ui/NoteDetailScreen.kt`).
- A stubbed export action (`export/NoteExporter.kt`) that currently throws `NotImplementedError`.

## Layout
```
src/commonMain/kotlin/com/example/notes/
  model/Note.kt                 data class + tag enum
  data/NoteRepository.kt        repository interface
  data/InMemoryNoteRepository.kt in-memory impl (no persistence)
  ui/NoteListScreen.kt          list of notes
  ui/NoteDetailScreen.kt        view/edit one note
  export/NoteExporter.kt        stubbed export (TODO)
```

## Notes for contributors
We try to use **Kotlin Multiplatform** libraries so features work on all three targets, not just
Android. When adding a capability, prefer a library that publishes common/native/jvm artifacts.
