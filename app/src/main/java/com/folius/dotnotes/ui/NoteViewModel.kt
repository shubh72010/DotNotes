package com.folius.dotnotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.folius.dotnotes.data.AppDatabase
import com.folius.dotnotes.data.Note
import com.folius.dotnotes.data.SettingsManager
import com.folius.dotnotes.network.ChatRequest
import com.folius.dotnotes.network.Message
import com.folius.dotnotes.network.OpenRouterApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

enum class NoteTab {
    NOTES, CHECKLISTS, SEARCH
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

    val apiKey = settingsManager.apiKey.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val modelId = settingsManager.modelId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "google/gemini-flash-1.5")
    val theme = settingsManager.themePref.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")
    val isAnimationsEnabled = settingsManager.isAnimationsEnabled.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val storageUri = settingsManager.storageUri.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    
    val folders: StateFlow<List<com.folius.dotnotes.data.Folder>> = folderDao.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenRouterApi::class.java)

    val notes: StateFlow<List<Note>> = combine(
        noteDao.getAllNotes(),
        _searchQuery,
        _selectedFolderId,
        _selectedTab,
        _isLoading
    ) { notes, query, folderId, tab, loading ->
        if (loading) return@combine emptyList<Note>()
        
        var filteredNotes = notes
        
        // Filter by Tab
        filteredNotes = filteredNotes.filter { 
            when (tab) {
                NoteTab.NOTES -> !it.isChecklist && !it.isPinned
                NoteTab.CHECKLISTS -> it.isChecklist && !it.isPinned
                NoteTab.SEARCH -> true // Show all in Search
            }
        }

        if (query.isNotBlank()) {
            filteredNotes = filteredNotes.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.content.contains(query, ignoreCase = true) 
            }
        }
        if (folderId != null) {
            filteredNotes = filteredNotes.filter { it.folderId == folderId }
        }
        filteredNotes = filteredNotes.filter { !it.isSecret }
        filteredNotes
    }.flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secretNotes: StateFlow<List<Note>> = combine(
        noteDao.getAllNotes(),
        _searchQuery,
        _isLoading
    ) { notes, query, loading ->
        if (loading) return@combine emptyList<Note>()
        
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


    fun setLoaded() {
        _isLoading.value = false
    }

    fun setSelectedFolder(id: Int?) {
        _selectedFolderId.value = id
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

    fun backupNotes(context: android.content.Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val uri = storageUri.value
            if (uri == null) {
                onError("Please set a backup directory in settings first.")
                return@launch
            }

            val allNotes = noteDao.getAllNotes().first()
            val allFolders = folderDao.getAllFolders().first()

            val success = com.folius.dotnotes.utils.BackupUtils.exportNotesToDirectory(
                context, uri, allNotes, allFolders
            )

            if (success) onSuccess() else onError("Failed to create backup.")
        }
    }

    suspend fun summarizeNote(content: String): String? {
        val key = apiKey.value ?: return "Error: API Key not set in Settings"
        val model = modelId.value
        
        return try {
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
            response.choices.firstOrNull()?.message?.content
        } catch (e: Exception) {
            "Error: ${e.localizedMessage}"
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun setSelectedTab(tab: NoteTab) {
        _selectedTab.value = tab
    }

    suspend fun saveNote(
        id: Int?,
        title: String,
        content: String,
        images: List<String>,
        isChecklist: Boolean,
        checklist: List<com.folius.dotnotes.data.ChecklistItem>,
        folderId: Int?,
        isSecret: Boolean = false,
        isPinned: Boolean = false
    ): Int {
        val note = if (id != null && id != -1) {
            Note(
                id = id,
                title = title,
                content = content,
                images = images,
                isChecklist = isChecklist,
                checklist = checklist,
                folderId = folderId,
                isSecret = isSecret,
                isPinned = isPinned,
                timestamp = System.currentTimeMillis()
            )
        } else {
            Note(
                title = title,
                content = content,
                images = images,
                isChecklist = isChecklist,
                checklist = checklist,
                folderId = folderId,
                isSecret = isSecret,
                isPinned = isPinned
            )
        }
        val insertedId = noteDao.insertNote(note)
        return if (id != null && id != -1) id else insertedId.toInt()
    }

    fun addNote(
        title: String,
        content: String,
        images: List<String> = emptyList(),
        isChecklist: Boolean = false,
        checklist: List<com.folius.dotnotes.data.ChecklistItem> = emptyList(),
        folderId: Int? = _selectedFolderId.value,
        isSecret: Boolean = false,
        isPinned: Boolean = false
    ) {
        viewModelScope.launch {
            noteDao.insertNote(
                Note(
                    title = title,
                    content = content,
                    images = images,
                    isChecklist = isChecklist,
                    checklist = checklist,
                    folderId = folderId,
                    isSecret = isSecret,
                    isPinned = isPinned
                )
            )
        }
    }

    fun toggleNotePinnedStatus(note: Note) {
        viewModelScope.launch {
            noteDao.insertNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.insertNote(note)
        }
    }

    fun toggleNoteSecretStatus(note: Note) {
        viewModelScope.launch {
            noteDao.insertNote(note.copy(isSecret = !note.isSecret))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun deleteNoteById(id: Int) {
        viewModelScope.launch {
            noteDao.deleteNoteById(id)
        }
    }

    suspend fun getNoteById(id: Int): Note? {
        return noteDao.getNoteById(id)
    }
}
