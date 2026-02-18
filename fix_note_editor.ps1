
$path = 'app\src\main\java\com\folius\dotnotes\ui\NoteEditorScreen.kt'
$lines = Get-Content $path

# Fix manual save (indices 347 to 360)
$newManualSave = @(
    "                                 viewModel.saveNote(",
    "                                     id = currentNoteId,",
    "                                     title = title,",
    "                                     content = content,",
    "                                     images = images,",
    "                                     isChecklist = isChecklist,",
    "                                     checklist = checklist,",
    "                                     folderId = folderId,",
    "                                     isSecret = isSecret,",
    "                                     isPinned = isPinned,",
    "                                     linkedNoteIds = linkedNoteIds,",
    "                                     tags = tags,",
    "                                     color = noteColor,",
    "                                     isMap = isMap,",
    "                                     mapItems = mapItems",
    "                                 )"
)

# Fix conversion block (indices 529 to 551)
$newConvBlock = @(
    "                                                 val newId = viewModel.saveNote(",
    "                                                     id = currentNoteId,",
    "                                                     title = title,",
    "                                                     content = content,",
    "                                                     images = images,",
    "                                                     isChecklist = isChecklist,",
    "                                                     checklist = checklist,",
    "                                                     folderId = folderId,",
    "                                                     isSecret = isSecret,",
    "                                                     isPinned = isPinned,",
    "                                                     linkedNoteIds = linkedNoteIds,",
    "                                                     tags = tags,",
    "                                                     color = noteColor,",
    "                                                     isMap = true,",
    "                                                     mapItems = mapItems",
    "                                                 )",
    "                                                 if (currentNoteId == null || currentNoteId == -1) {",
    "                                                     currentNoteId = newId",
    "                                                 }",
    "                                                 isMap = true"
)

# Reconstruct the file
$newLines = $lines[0..346] + $newManualSave + $lines[361..528] + $newConvBlock + $lines[552..($lines.Count-1)]

$newLines | Set-Content $path -Encoding UTF8
