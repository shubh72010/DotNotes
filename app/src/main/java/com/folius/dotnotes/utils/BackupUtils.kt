package com.folius.dotnotes.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.folius.dotnotes.data.ChecklistItem
import com.folius.dotnotes.data.Note
import com.folius.dotnotes.data.Folder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupUtils {

    private val gson = Gson()

    fun exportNotesToDirectory(
        context: Context,
        uriString: String,
        notes: List<Note>,
        folders: List<Folder>
    ): Boolean {
        return try {
            val rootUri = Uri.parse(uriString)
            val rootDoc = DocumentFile.fromTreeUri(context, rootUri) ?: return false
            
            if (!rootDoc.exists() || !rootDoc.canWrite()) return false

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupDirName = "DotNotes_Backup_$timestamp"
            val backupDir = rootDoc.createDirectory(backupDirName) ?: return false

            val folderMap = folders.associateBy { it.id }
            val notesByFolder = notes.groupBy { it.folderId }

            // Export Notes without folder
            notesByFolder[null]?.forEach { note ->
                saveNoteToFile(context, backupDir, note)
            }

            // Export Folders and their notes
            folders.forEach { folder ->
                val folderDir = backupDir.createDirectory(folder.name.replace("/", "_")) ?: return@forEach
                notesByFolder[folder.id]?.forEach { note ->
                    saveNoteToFile(context, folderDir, note)
                }
            }

            // Also export a JSON manifest for full restore
            val jsonFile = backupDir.createFile("application/json", "backup_manifest.json")
            jsonFile?.let { file ->
                val manifest = BackupManifest(notes = notes, folders = folders)
                context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
                    outputStream.write(gson.toJson(manifest).toByteArray())
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun saveNoteToFile(context: Context, parentDir: DocumentFile, note: Note) {
        val fileName = "${note.title.replace("/", "_").ifBlank { "Untitled" }}.txt"
        val file = parentDir.createFile("text/plain", fileName) ?: return
        
        val content = buildString {
            append("Title: ${note.title}\n")
            append("Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(note.timestamp))}\n")
            append("---------------------------\n\n")
            
            if (note.isChecklist) {
                note.checklist.forEach { item ->
                    append("${if (item.isChecked) "[x]" else "[ ]"} ${item.text}\n")
                }
            } else {
                append(note.content)
            }
        }

        context.contentResolver.openOutputStream(file.uri)?.use { outputStream ->
            outputStream.write(content.toByteArray())
        }
    }

    // ─── Import Logic ───────────────────────────────────────────

    /**
     * Import notes from a list of file URIs. Supports .txt, .md, and .json files.
     * Returns a list of Note objects ready for insertion.
     */
    fun importNotesFromUris(context: Context, uris: List<Uri>): List<Note> {
        val notes = mutableListOf<Note>()
        for (uri in uris) {
            try {
                val fileName = getFileName(context, uri) ?: "Untitled"
                val extension = fileName.substringAfterLast('.', "txt").lowercase()
                val content = readFileContent(context, uri)

                when (extension) {
                    "json" -> {
                        notes.addAll(parseJsonBackup(content))
                    }
                    "md", "txt" -> {
                        val title = fileName.substringBeforeLast('.')
                        // Detect checklist pattern
                        val lines = content.lines()
                        val isChecklist = lines.any { it.trimStart().startsWith("[ ]") || it.trimStart().startsWith("[x]") }
                        
                        if (isChecklist) {
                            val checklistItems = lines.filter { 
                                it.trimStart().startsWith("[ ]") || it.trimStart().startsWith("[x]")
                            }.map { line ->
                                val trimmed = line.trimStart()
                                val checked = trimmed.startsWith("[x]")
                                val text = trimmed.removePrefix("[x]").removePrefix("[ ]").trim()
                                ChecklistItem(text = text, isChecked = checked)
                            }
                            notes.add(Note(
                                title = title,
                                content = "",
                                isChecklist = true,
                                checklist = checklistItems
                            ))
                        } else {
                            notes.add(Note(
                                title = title,
                                content = content
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return notes
    }

    private fun parseJsonBackup(json: String): List<Note> {
        return try {
            // Try BackupManifest format first
            val manifest = gson.fromJson(json, BackupManifest::class.java)
            manifest.notes.map { note ->
                // Reset IDs so they get auto-generated
                note.copy(id = 0, isDeleted = false, deletedTimestamp = null)
            }
        } catch (e: Exception) {
            try {
                // Try raw list of notes
                val listType = object : TypeToken<List<Note>>() {}.type
                val notes: List<Note> = gson.fromJson(json, listType)
                notes.map { it.copy(id = 0) }
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }

    private fun readFileContent(context: Context, uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).readText()
        } ?: ""
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    data class BackupManifest(
        val notes: List<Note>,
        val folders: List<Folder>
    )
}
