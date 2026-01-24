package com.folius.dotnotes.utils

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.folius.dotnotes.data.Note
import com.folius.dotnotes.data.Folder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupUtils {
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

            // Create a backup folder with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupDirName = "DotNotes_Backup_$timestamp"
            val backupDir = rootDoc.createDirectory(backupDirName) ?: return false

            // Map folders for easy lookup
            val folderMap = folders.associateBy { it.id }

            // Group notes by folder
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
}
