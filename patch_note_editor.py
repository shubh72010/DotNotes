
import os
import sys

# Determine the correct path
path = r'app\src\main\java\com\folius\dotnotes\ui\NoteEditorScreen.kt'
if not os.path.exists(path):
    # Try absolute path if relative fails
    path = r'c:\Users\Shubh\AndroidStudioProjects\DotNotes\app\src\main\java\com\folius\dotnotes\ui\NoteEditorScreen.kt'

if not os.path.exists(path):
    print(f"Error: File not found at {path}")
    sys.exit(1)

with open(path, 'r', encoding='utf-8') as f:
    original_text = f.read()

# Normalize to LF for matching, then we will write back as CRLF for Windows
text = original_text.replace('\r\n', '\n')

# 1. Fix manual save (around line 360)
old_manual_lines = """                                     tags = tags,
                                     color = noteColor
                                 )"""
new_manual_lines = """                                     tags = tags,
                                     color = noteColor,
                                     isMap = isMap,
                                     mapItems = mapItems
                                 )"""

# 2. Fix corrupted conversion block
old_conv_lines = """                                                 viewModel.saveNote(
                                                     id = currentNoteId,
                                                     title = title,
                                                     content = content,
                                                     images = images,
                                                     isChecklist = isChecklist,
                                                     checklist = checklist,
                                                     folderId = folderId,
                                                     isSecret = isSecret,
                                                     isPinned = isPinned,
                                                     linkedNoteIds = linkedNoteIds,
                                                     tags = tags,
                                                     color = noteColor,
                                      isMap = isMap,
                                      mapItems = mapItems,
                                      isMap = isMap,
                                      mapItems = mapItems,
                                                     if (currentNoteId == null || currentNoteId == -1) {
                                                      currentNoteId = newId
                                                  }
                                                  isMap = true
                                                 )
                                                 isMap = true"""

new_conv_lines = """                                                 val newId = viewModel.saveNote(
                                                     id = currentNoteId,
                                                     title = title,
                                                     content = content,
                                                     images = images,
                                                     isChecklist = isChecklist,
                                                     checklist = checklist,
                                                     folderId = folderId,
                                                     isSecret = isSecret,
                                                     isPinned = isPinned,
                                                     linkedNoteIds = linkedNoteIds,
                                                     tags = tags,
                                                     color = noteColor,
                                                     isMap = true,
                                                     mapItems = mapItems
                                                 )
                                                 if (currentNoteId == null || currentNoteId == -1) {
                                                     currentNoteId = newId
                                                 }
                                                 isMap = true"""

# Perform replacements
patched_text = text.replace(old_manual_lines, new_manual_lines)
patched_text = patched_text.replace(old_conv_lines, new_conv_lines)

if patched_text == text:
    print("Warning: No exact replacements were made.")
else:
    print("Replacements made successfully.")

# Write back with Windows line endings
final_text = patched_text.replace('\n', '\r\n')
with open(path, 'w', encoding='utf-8', newline='') as f:
    f.write(final_text)

print("Successfully patched NoteEditorScreen.kt")
