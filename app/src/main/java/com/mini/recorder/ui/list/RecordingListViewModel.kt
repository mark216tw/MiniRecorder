package com.mini.recorder.ui.list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mini.recorder.data.RecordingRepository
import com.mini.recorder.data.db.Recording
import com.mini.recorder.data.db.RecordingWithTags
import com.mini.recorder.playback.PlaybackController
import com.mini.recorder.recording.RecordingStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RecordingSortOrder(val label: String) {
    DATE_DESC("最新優先"),
    DATE_ASC("最舊優先"),
    DURATION_DESC("時間最長"),
    TITLE_ASC("標題排序")
}

@HiltViewModel
class RecordingListViewModel @Inject constructor(
    private val repository: RecordingRepository,
    private val playback: PlaybackController,
    recordingState: RecordingStateHolder
) : ViewModel() {

    val isRecording: StateFlow<Boolean> = recordingState.state
        .map { it.isRecording && !it.isPaused }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val activeRecordingId: StateFlow<Long> = recordingState.state
        .map { it.recordingId }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)

    private val _filterTags = MutableStateFlow<Set<String>>(emptySet())
    val filterTags: StateFlow<Set<String>> = _filterTags.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(RecordingSortOrder.DATE_DESC)
    val sortOrder: StateFlow<RecordingSortOrder> = _sortOrder.asStateFlow()

    // 3.2 搜尋與排序組合 Flow
    val recordings: StateFlow<List<RecordingWithTags>> =
        combine(
            repository.observeAll(),
            _filterTags,
            _searchQuery,
            _sortOrder,
            activeRecordingId
        ) { list, tags, query, order, activeId ->
            var filtered = if (activeId > 0L) list.filter { it.recording.id != activeId } else list
            if (tags.isNotEmpty()) {
                filtered = filtered.filter { item ->
                    tags.all { name -> item.tags.any { it.name == name } }
                }
            }
            if (query.isNotBlank()) {
                val q = query.trim()
                filtered = filtered.filter { item ->
                    item.recording.title.contains(q, ignoreCase = true)
                }
            }
            when (order) {
                RecordingSortOrder.DATE_DESC -> filtered.sortedByDescending { it.recording.createdAt }
                RecordingSortOrder.DATE_ASC -> filtered.sortedBy { it.recording.createdAt }
                RecordingSortOrder.DURATION_DESC -> filtered.sortedByDescending { it.recording.durationMs }
                RecordingSortOrder.TITLE_ASC -> filtered.sortedBy { it.recording.title }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: RecordingSortOrder) {
        _sortOrder.value = order
    }

    fun toggleFilter(name: String) {
        _filterTags.value =
            if (name in _filterTags.value) _filterTags.value - name else _filterTags.value + name
    }

    fun removeFilter(name: String) {
        _filterTags.value = _filterTags.value - name
    }

    fun clearFilters() {
        _filterTags.value = emptySet()
    }

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    fun importAudio(uri: Uri) {
        viewModelScope.launch {
            _importMessage.value = if (repository.importAudio(uri) != null) "已匯入音樂檔案" else "匯入失敗"
        }
    }

    fun clearImportMessage() {
        _importMessage.value = null
    }

    fun play(recording: Recording) {
        if (isRecording.value) return
        playback.play(recording, autoplay = true)
    }

    fun pausePlayback() = playback.pause()
}
