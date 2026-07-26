package com.loomora.feature.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.audio.editor.AudioEditExporter
import com.loomora.core.model.EditOperation
import com.loomora.core.model.EditRecipe
import com.loomora.core.model.Recording
import com.loomora.core.model.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val recording: Recording? = null,
    val recipe: EditRecipe = EditRecipe(""),
    val selectionStartMs: Long = 0L,
    val selectionEndMs: Long = 0L,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isExporting: Boolean = false,
    val exportSuccessMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
    private val audioEditExporter: AudioEditExporter
) : ViewModel() {

    private val recordingId: String? = savedStateHandle["recordingId"]
    private val _recordingIdFlow = MutableStateFlow(recordingId)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recordingFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) recordingRepository.getRecordingById(id) else flowOf(null)
    }

    private val undoStack = mutableListOf<List<EditOperation>>()
    private val redoStack = mutableListOf<List<EditOperation>>()

    private val _currentOperations = MutableStateFlow<List<EditOperation>>(emptyList())
    private val _isSpeechClarityEnabled = MutableStateFlow(false)
    private val _selectionStartMs = MutableStateFlow(0L)
    private val _selectionEndMs = MutableStateFlow(0L)
    private val _isExporting = MutableStateFlow(false)
    private val _exportSuccessMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EditorUiState> = combine(
        combine(recordingFlow, _currentOperations, _isSpeechClarityEnabled) { rec, ops, clarity ->
            Triple(rec, ops, clarity)
        },
        combine(_selectionStartMs, _selectionEndMs, _isExporting, _exportSuccessMessage) { selStart, selEnd, exporting, successMsg ->
            Four(selStart, selEnd, exporting, successMsg)
        }
    ) { (recording, ops, clarity), (selStart, selEnd, exporting, successMsg) ->
        val recipe = EditRecipe(
            originalRecordingId = recording?.id ?: "",
            operations = ops,
            isSpeechClarityEnabled = clarity
        )

        val finalSelEnd = if (selEnd == 0L && recording != null) recording.durationMs else selEnd

        EditorUiState(
            recording = recording,
            recipe = recipe,
            selectionStartMs = selStart,
            selectionEndMs = finalSelEnd,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            isExporting = exporting,
            exportSuccessMessage = successMsg,
            errorMessage = _errorMessage.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EditorUiState()
    )

    fun updateSelection(startMs: Long, endMs: Long) {
        _selectionStartMs.value = startMs.coerceAtLeast(0L)
        _selectionEndMs.value = endMs.coerceAtLeast(_selectionStartMs.value)
    }

    fun applyTrim() {
        val start = _selectionStartMs.value
        val end = _selectionEndMs.value
        if (end <= start) return

        pushToUndoStack()
        val newOps = _currentOperations.value.toMutableList().apply {
            add(EditOperation.Trim(start, end))
        }
        _currentOperations.value = newOps
    }

    fun applyDeleteSelection() {
        val start = _selectionStartMs.value
        val end = _selectionEndMs.value
        if (end <= start) return

        pushToUndoStack()
        val newOps = _currentOperations.value.toMutableList().apply {
            add(EditOperation.DeleteRange(start, end))
        }
        _currentOperations.value = newOps
    }

    fun toggleSpeechClarity() {
        _isSpeechClarityEnabled.value = !_isSpeechClarityEnabled.value
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_currentOperations.value)
            _currentOperations.value = undoStack.removeAt(undoStack.lastIndex)
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_currentOperations.value)
            _currentOperations.value = redoStack.removeAt(redoStack.lastIndex)
        }
    }

    private fun pushToUndoStack() {
        undoStack.add(_currentOperations.value)
        redoStack.clear()
    }

    fun exportRecording() {
        val recording = uiState.value.recording ?: return
        viewModelScope.launch {
            _isExporting.value = true
            val result = audioEditExporter.exportEditedRecording(recording, uiState.value.recipe)
            _isExporting.value = false
            if (result.isSuccess) {
                _exportSuccessMessage.value = "Edited file saved successfully as new recording!"
            } else {
                _errorMessage.value = result.exceptionOrNull()?.localizedMessage ?: "Export failed"
            }
        }
    }
}

private data class Four<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
