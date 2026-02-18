package com.folius.dotnotes.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.folius.dotnotes.data.AppDatabase
import com.folius.dotnotes.data.Note
import com.folius.dotnotes.data.SettingsManager
import com.folius.dotnotes.network.ChatRequest
import com.folius.dotnotes.network.Message
import com.folius.dotnotes.network.OpenRouterApi
import com.folius.dotnotes.utils.BackupUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class NoteTab {
    NOTES, CHECKLISTS, SEARCH
}

enum class SortOrder(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    TITLE_ASC("Title A→Z"),
    TITLE_DESC("Title Z→A"),
    MODIFIED("Last Modified")
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val noteDao = db.noteDao()
    private val folderDao = db.folderDao()
    private val settingsManager = SettingsManager(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedFolderId = MutableStateFlow<Int?>(null)
    val selectedFolderId: StateFlow<Int?> = _selectedFolderId

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _selectedTab = MutableStateFlow(NoteTab.NOTES)
    val selectedTab: StateFlow<NoteTab> = _selectedTab

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _selectedTag = MutableStateFlow<String?>(null)
    val selectedTag: StateFlow<String?> = _selectedTag

    private val _isSecretAuthenticated = MutableStateFlow(false)
    val isSecretAuthenticated: StateFlow<Boolean> = _isSecretAuthenticated

    val apiKey = settingsManager.apiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val modelId = settingsManager.modelId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "google/gemini-flash-1.5")
    val theme = settingsManager.themePref.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Dark")
    val isAnimationsEnabled = settingsManager.isAnimationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val storageUri = settingsManager.storageUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val folders: StateFlow<List<com.folius.dotnotes.data.Folder>> = folderDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1/"
        
        private val retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        private val api by lazy { retrofit.create(OpenRouterApi::class.java) }
    }

    // ─── Main notes flow (Refactored to typed nested combine) ──────

    val allNonDeletedNotes: StateFlow<List<Note>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val filterCombiner = combine(
        _searchQuery,
        _selectedFolderId,
        _selectedTab
    ) { query, folderId, tab ->
        Triple(query, folderId, tab)
    }

    private val sortCombiner = combine(
        _sortOrder,
        _selectedTag
    ) { sort, tag ->
        sort to tag
    }

    val notes: StateFlow<List<Note>> = combine(
        allNonDeletedNotes,
        filterCombiner,
        sortCombiner
    ) { notes, filters, sorting ->
        val (query, folderId, tab) = filters
        val (sort, tag) = sorting

        var filteredNotes = notes

        // Filter by Tab
        filteredNotes = filteredNotes.filter { note ->
            when (tab) {
                NoteTab.NOTES -> !note.isChecklist && !note.isPinned
                NoteTab.CHECKLISTS -> note.isChecklist && !note.isPinned
                NoteTab.SEARCH -> true
            }
        }

        // Filter by search query
        if (query.isNotBlank()) {
            filteredNotes = filteredNotes.filter { note ->
                note.title.contains(query, ignoreCase = true) || 
                note.content.contains(query, ignoreCase = true) ||
                note.tags.any { t -> t.contains(query, ignoreCase = true) }
            }
        }

        // Filter by folder
        if (folderId != null) {
            filteredNotes = filteredNotes.filter { note -> note.folderId == folderId }
        }

        // Filter by tag
        if (tag != null) {
            filteredNotes = filteredNotes.filter { note -> tag in note.tags }
        }

        // Exclude secret notes
        filteredNotes = filteredNotes.filter { note -> !note.isSecret }

        // Apply sort
        filteredNotes = when (sort) {
            SortOrder.DATE_DESC -> filteredNotes.sortedByDescending { note -> note.timestamp }
            SortOrder.DATE_ASC -> filteredNotes.sortedBy { note -> note.timestamp }
            SortOrder.TITLE_ASC -> filteredNotes.sortedBy { note -> note.title.lowercase() }
            SortOrder.TITLE_DESC -> filteredNotes.sortedByDescending { note -> note.title.lowercase() }
            SortOrder.MODIFIED -> filteredNotes.sortedByDescending { note -> note.lastModified }
        }

        filteredNotes
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secretNotes: StateFlow<List<Note>> = combine(
        allNonDeletedNotes,
        _searchQuery
    ) { notes, query ->
        var filteredNotes = notes.filter { it.isSecret }
        if (query.isNotBlank()) {
            filteredNotes = filteredNotes.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.content.contains(query, ignoreCase = true) 
            }
        }
        filteredNotes
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedNotes: StateFlow<List<Note>> = noteDao.getPinnedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedNotes: StateFlow<List<Note>> = noteDao.getDeletedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All unique tags across all notes
    val allTags: StateFlow<List<String>> = allNonDeletedNotes
        .map { notes -> notes.flatMap { it.tags }.distinct().sorted() }
        .flowOn(kotlinx.coroutines.Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Auto-cleanup policy: permanently delete trash typically older than 30 days
    init {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
            noteDao.permanentlyDeleteOldTrash(thirtyDaysAgo)
        }
    }

    fun setLoaded() {
        _isLoading.value = false
    }

    fun setSelectedFolder(id: Int?) {
        _selectedFolderId.value = id
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSelectedTag(tag: String?) {
        _selectedTag.value = tag
    }

    fun setSecretAuthenticated(authenticated: Boolean) {
        _isSecretAuthenticated.value = authenticated
    }

    fun addFolder(name: String) {
        viewModelScope.launch {
            folderDao.insertFolder(com.folius.dotnotes.data.Folder(name = name))
        }
    }

    fun renameFolder(folder: com.folius.dotnotes.data.Folder, newName: String) {
        viewModelScope.launch {
            folderDao.updateFolder(folder.copy(name = newName))
        }
    }

    fun deleteFolder(folder: com.folius.dotnotes.data.Folder) {
        viewModelScope.launch {
            folderDao.deleteFolder(folder)
            if (_selectedFolderId.value == folder.id) {
                _selectedFolderId.value = null
            }
        }
    }

    fun saveSettings(key: String, model: String, theme: String, animationsEnabled: Boolean) {
        viewModelScope.launch { settingsManager.saveSettings(key, model, theme, animationsEnabled) }
    }

    fun setStorageUri(uri: String?) {
        viewModelScope.launch { settingsManager.saveStorageUri(uri) }
    }

    fun backupNotes(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val uri = storageUri.value
            if (uri == null) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onError("Please set a backup directory in settings first.")
                }
                return@launch
            }

            val allNotes = noteDao.getAllNotes().first()
            val allFolders = folderDao.getAllFolders().first()

            val success = BackupUtils.exportNotesToDirectory(
                getApplication(), uri, allNotes, allFolders
            )

            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (success) onSuccess() else onError("Failed to create backup.")
            }
        }
    }

    fun summarizeNote(content: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            // Read settings within coroutine to handle initialization delays
            val key = apiKey.value
            if (key == null) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult("Error: API Key not set in Settings")
                }
                return@launch
            }
            val model = modelId.value
            
            try {
                val response = api.getCompletion(
                    apiKey = "Bearer $key",
                    request = ChatRequest(
                        model = model,
                        messages = listOf(
                            Message("system", "Summarize the following note content concisely."),
                            Message("user", content)
                        )
                    )
                )
                val result = response.choices.firstOrNull()?.message?.content
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(result)
                }
            } catch (e: Exception) {
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult("Error: ${e.localizedMessage}")
                }
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setSelectedTab(tab: NoteTab) {
        _selectedTab.value = tab
    }

    /**
     * Unified method for adding or updating a note.
     * 
     * @param id The ID of the note to update, or null for a new note.
     * @param color The new color to set. If null, the existing color is maintained for updates.
     *              For new notes, null results in no color.
     */
    suspend fun saveNote(
        id: Int?,
        title: String,
        content: String,
        images: List<String> = emptyList(),
        isChecklist: Boolean = false,
        checklist: List<com.folius.dotnotes.data.ChecklistItem> = emptyList(),
        folderId: Int? = _selectedFolderId.value,
        isSecret: Boolean? = null,
        isPinned: Boolean? = null,
        linkedNoteIds: List<Int>? = null,
        tags: List<String>? = null,
        color: Int? = null,
        isMap: Boolean? = null,
        mapItems: List<com.folius.dotnotes.data.MapItem>? = null
    ): Int {
        val now = System.currentTimeMillis()
        val note = if (id != null && id != -1) {
            val existing = noteDao.getNoteById(id) ?: return -1
            existing.copy(
                title = title,
                content = content,
                images = images,
                isChecklist = isChecklist,
                checklist = checklist,
                folderId = folderId,
                isSecret = isSecret ?: existing.isSecret,
                isPinned = isPinned ?: existing.isPinned,
                lastModified = now,
                linkedNoteIds = linkedNoteIds ?: existing.linkedNoteIds,
                tags = tags ?: existing.tags,
                color = color ?: existing.color, // Passing null means "keep existing"
                isMap = isMap ?: existing.isMap,
                mapItems = mapItems ?: existing.mapItems
            )
        } else {
            Note(
                title = title,
                content = content,
                images = images,
                isChecklist = isChecklist,
                checklist = checklist,
                folderId = folderId,
                isSecret = isSecret ?: false,
                isPinned = isPinned ?: false,
                timestamp = now,
                lastModified = now,
                linkedNoteIds = linkedNoteIds ?: emptyList(),
                tags = tags ?: emptyList(),
                color = color,
                isMap = isMap ?: false,
                mapItems = mapItems ?: emptyList()
            )
        }
        val insertedId = noteDao.insertNote(note)
        return if (id != null && id != -1) id else insertedId.toInt()
    }

    /**
     * Helper to launch a note save from the UI without managing coroutines.
     * Consolidates add/save logic.
     */
    fun addNote(
        title: String,
        content: String,
        images: List<String> = emptyList(),
        isChecklist: Boolean = false,
        checklist: List<com.folius.dotnotes.data.ChecklistItem> = emptyList(),
        folderId: Int? = _selectedFolderId.value,
        isSecret: Boolean = false,
        isPinned: Boolean = false,
        isMap: Boolean = false,
        mapItems: List<com.folius.dotnotes.data.MapItem> = emptyList()
    ) {
        viewModelScope.launch {
            saveNote(
                id = null,
                title = title,
                content = content,
                images = images,
                isChecklist = isChecklist,
                checklist = checklist,
                folderId = folderId,
                isSecret = isSecret,
                isPinned = isPinned,
                isMap = isMap,
                mapItems = mapItems
            )
        }
    }

    fun createMapNote(title: String, folderId: Int? = _selectedFolderId.value) {
        addNote(
            title = title,
            content = "",
            isMap = true,
            folderId = folderId
        )
    }

    fun updateMapItems(noteId: Int, items: List<com.folius.dotnotes.data.MapItem>) {
        viewModelScope.launch {
            val note = noteDao.getNoteById(noteId)
            if (note != null) {
                noteDao.insertNote(note.copy(mapItems = items, lastModified = System.currentTimeMillis()))
            }
        }
    }

    fun toggleNotePinnedStatus(note: Note) {
        viewModelScope.launch {
            noteDao.insertNote(note.copy(isPinned = !note.isPinned))
        }
    }


    fun toggleNoteSecretStatus(note: Note) {
        viewModelScope.launch {
            noteDao.insertNote(note.copy(isSecret = !note.isSecret))
        }
    }

    // ─── Trash (Soft Delete) ────────────────────────────────────

    /**
     * Moves a note to the trash (soft delete).
     */
    fun trashNote(note: Note) {
        viewModelScope.launch {
            noteDao.softDeleteNote(note.id, System.currentTimeMillis())
        }
    }

    /**
     * Moves a note to the trash by its ID. Useful when the full Note object isn't available.
     */
    fun trashNoteById(id: Int) {
        viewModelScope.launch {
            noteDao.softDeleteNote(id, System.currentTimeMillis())
        }
    }

    fun restoreNote(note: Note) {
        viewModelScope.launch {
            noteDao.restoreNote(note.id)
        }
    }

    fun permanentlyDeleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteDao.emptyTrash()
        }
    }

    // ─── Duplicate Note ─────────────────────────────────────────

    fun duplicateNote(note: Note) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            noteDao.insertNote(
                note.copy(
                    id = 0,
                    title = "${note.title} (Copy)",
                    timestamp = now,
                    lastModified = now
                )
            )
        }
    }

    // ─── Import Notes ───────────────────────────────────────────

    fun importNotes(uris: List<Uri>, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val notes = BackupUtils.importNotesFromUris(getApplication(), uris)
            if (notes.isNotEmpty()) {
                noteDao.insertNotes(notes)
            }
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                onResult(notes.size)
            }
        }
    }

    // ─── Notes Connect (Linking) ────────────────────────────────

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }

    suspend fun getNoteByTitle(title: String): Note? {
        return noteDao.getNoteByTitle(title)
    }

    suspend fun getLinkedNotes(noteId: Int): List<Note> {
        val note = noteDao.getNoteById(noteId) ?: return emptyList()
        if (note.linkedNoteIds.isEmpty()) return emptyList()
        return noteDao.getNotesByIds(note.linkedNoteIds)
    }

    suspend fun getBacklinks(noteId: Int): List<Note> {
        // Query database directly to ensure fresh snapshot
        return noteDao.getAllNotes().first()
            .filter { noteId in it.linkedNoteIds && it.id != noteId }
    }
}
