# [Modernized Markdown & UI Stability]

## Completed
- **Recursive Markdown**: Implemented `MarkdownBlock` architecture with nested inline support (bold/italic/code/strikethrough).
- **Table Scaling**: Added horizontal scrolling and improved separator detection for complex data tables.
- **Locked Notes Fix**: Resolved "reappearing" bug by initializing `isSecret` from DB in `NoteEditorScreen` and adding unique `LazyColumn` keys.
- **Gesture UX**: Fixed white glitch in predictive back gesture with consistent Scaffold background.

## Current System Architecture
- **Markdown Layer**: `MarkdownRenderer.kt` (Parser) -> `MarkdownBlock` (Data Model) -> `MarkdownBlockView.kt` (Renderer).
- **Note State**: `NoteViewModel.kt` manages `secretNotes` and `notes` Flows, now strictly synced with Editor state on load.
- **UI Stability**: All lists use `items(key = { it.id })` to ensure stable recomposition during state changes.

## Next Concrete Priorities
1. User verification of the "Unlock" note flow in `SecretNotesScreen`.
2. Final audit of Markdown performance for extremely large notes.
3. Refining AI Assist prompt context if search results are too broad.
