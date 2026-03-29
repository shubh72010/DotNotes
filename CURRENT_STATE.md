# Current State - DotNotes

## Project Summary
DotNotes has undergone a comprehensive structural and UX overhaul based on a deep audit. The application now follows a consistent "Pill-centric" design identity across all primary and secondary screens. Architecture has been modernized with a clean Repository pattern and decoupled network module. UX has been refined with expressive `spring` motion and full Material You theme compliance. The build is fully verified and stable.

## System Architecture
- **Data Layer**: Room DB (v10) with indices on `timestamp` and `lastModified`. Data access is encapsulated in `NoteRepository.kt`.
- **Infrastructure**: `NetworkModule.kt` provides singleton access to the AI service client.
- **UI Architecture**: Pill-centric navigation is universal (`NoteList`, `NoteEditor`, `NoteMap`, `Settings`, `Trash`, `SecretNotes`). Editor actions (Share, Image, Checklist, Color, Tags, Lock, Pin) are unified in the pill menu.
- **Organization**: Folders have been replaced by the **NoteMap** system. Notes can be linked to Map notes via the long-press menu or the editor's "Add to Map" action.
- **Design System**: M3 compliant. Hardcoded shapes and colors have been replaced with `MaterialTheme` tokens.

## Next Priorities
1. **Editor State to ViewModel**: Move ~40 `remember` state variables to ViewModel with `SavedStateHandle` to survive config changes and improve testability.
2. **Shared Transitions**: Implement Material 3 shared element transitions between note list and editor for a seamless "growing" motion.
3. **Advanced Search**: Implement in-content highlighting and fuzzy search capabilities within the `NoteListScreen`.
