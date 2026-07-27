package com.loomora.feature.editor

import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.audio.editor.AudioEditExporter
import com.loomora.core.audio.editor.AudioEditException
import com.loomora.core.audio.storage.RecordingStorageManager
import com.loomora.core.audio.waveform.WaveformAlgorithm
import com.loomora.core.audio.waveform.WaveformLoadState
import com.loomora.core.audio.waveform.WaveformRepository
import com.loomora.core.model.EditRecipeIssue
import com.loomora.core.model.EditRecipeValidation
import com.loomora.core.model.EditOperation
import com.loomora.core.model.EditRecipe
import com.loomora.core.model.KeepRange
import com.loomora.core.model.Recording
import com.loomora.core.model.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditPreviewSegment(
    val startMs: Long,
    val endMs: Long
)

data class EditorUiState(
    val recording: Recording? = null,
    val recipe: EditRecipe = EditRecipe(""),
    val selectionStartMs: Long = 0L,
    val selectionEndMs: Long = 0L,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Int? = null,
    val waveform: WaveformLoadState = WaveformLoadState.Idle,
    val previewSegments: List<EditPreviewSegment> = emptyList(),
    val previewDurationMs: Long = 0L,
    val validationIssue: EditRecipeIssue? = null,
    val lastExportedRecording: Recording? = null,
    val shareIntent: Intent? = null,
    val exportSuccessMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val recordingRepository: RecordingRepository,
    private val audioEditExporter: AudioEditExporter,
    private val waveformRepository: WaveformRepository,
    private val recordingStorageManager: RecordingStorageManager
) : ViewModel() {

    private val recordingId: String? = savedStateHandle["recordingId"]
    private val _recordingIdFlow = MutableStateFlow(recordingId)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val recordingFlow = _recordingIdFlow.flatMapLatest { id ->
        if (id != null) recordingRepository.getRecordingById(id) else flowOf(null)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val waveformFlow = recordingFlow.flatMapLatest { recording ->
        if (recording == null) {
            flowOf(WaveformLoadState.Idle)
        } else {
            waveformRepository.loadWaveform(recording, WaveformAlgorithm.EDITOR_RESOLUTION)
        }
    }

    private val undoStack = mutableListOf<List<EditOperation>>()
    private val redoStack = mutableListOf<List<EditOperation>>()
    private var exportJob: Job? = null

    private val _currentOperations = MutableStateFlow<List<EditOperation>>(emptyList())
    private val _isSpeechClarityEnabled = MutableStateFlow(false)
    private val _recipeRevision = MutableStateFlow(0L)
    private val _selectionStartMs = MutableStateFlow(0L)
    private val _selectionEndMs = MutableStateFlow(0L)
    private val _isExporting = MutableStateFlow(false)
    private val _exportProgress = MutableStateFlow<Int?>(null)
    private val _lastExportedRecording = MutableStateFlow<Recording?>(null)
    private val _shareIntent = MutableStateFlow<Intent?>(null)
    private val _exportSuccessMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<EditorUiState> = combine(
        combine(recordingFlow, _currentOperations, _isSpeechClarityEnabled, _recipeRevision) { rec, ops, clarity, revision ->
            RecipeStateBundle(rec, ops, clarity, revision)
        },
        combine(
            combine(_selectionStartMs, _selectionEndMs, _isExporting, _exportSuccessMessage, waveformFlow) { selStart, selEnd, exporting, successMsg, waveform ->
                ExportUiBundle(
                    selectionStartMs = selStart,
                    selectionEndMs = selEnd,
                    isExporting = exporting,
                    exportSuccessMessage = successMsg,
                    waveform = waveform
                )
            },
            combine(_exportProgress, _lastExportedRecording, _shareIntent) { exportProgress, lastExportedRecording, shareIntent ->
                ExportArtifactBundle(
                    exportProgress = exportProgress,
                    lastExportedRecording = lastExportedRecording,
                    shareIntent = shareIntent
                )
            }
        ) { exportUi, exportArtifacts ->
            ExportStateBundle(
                selectionStartMs = exportUi.selectionStartMs,
                selectionEndMs = exportUi.selectionEndMs,
                isExporting = exportUi.isExporting,
                exportSuccessMessage = exportUi.exportSuccessMessage,
                waveform = exportUi.waveform,
                exportProgress = exportArtifacts.exportProgress,
                lastExportedRecording = exportArtifacts.lastExportedRecording,
                shareIntent = exportArtifacts.shareIntent
            )
        }
    ) { recipeState, exportState ->
        val recording = recipeState.recording
        val recipe = EditRecipe(
            originalRecordingId = recording?.id ?: "",
            operations = recipeState.operations,
            isSpeechClarityEnabled = recipeState.isSpeechClarityEnabled,
            recipeRevision = recipeState.recipeRevision
        )
        val validation = recording?.let { recipe.validate(it.durationMs) }
        val previewSegments = when (validation) {
            is EditRecipeValidation.Valid -> validation.keepRanges.map { keepRange -> keepRange.toEditPreviewSegment() }
            else -> emptyList()
        }
        val previewDurationMs = when (validation) {
            is EditRecipeValidation.Valid -> validation.outputDurationMs
            else -> 0L
        }
        val validationIssue = (validation as? EditRecipeValidation.Invalid)?.issue

        val finalSelEnd = if (exportState.selectionEndMs == 0L && recording != null) recording.durationMs else exportState.selectionEndMs

        EditorUiState(
            recording = recording,
            recipe = recipe,
            selectionStartMs = exportState.selectionStartMs,
            selectionEndMs = finalSelEnd,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            isExporting = exportState.isExporting,
            exportProgress = exportState.exportProgress,
            waveform = exportState.waveform,
            previewSegments = previewSegments,
            previewDurationMs = previewDurationMs,
            validationIssue = validationIssue,
            lastExportedRecording = exportState.lastExportedRecording,
            shareIntent = exportState.shareIntent,
            exportSuccessMessage = exportState.exportSuccessMessage,
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
        bumpRecipeRevision()
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
        bumpRecipeRevision()
    }

    fun toggleSpeechClarity() {
        _isSpeechClarityEnabled.value = !_isSpeechClarityEnabled.value
        bumpRecipeRevision()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            redoStack.add(_currentOperations.value)
            _currentOperations.value = undoStack.removeAt(undoStack.lastIndex)
            bumpRecipeRevision()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            undoStack.add(_currentOperations.value)
            _currentOperations.value = redoStack.removeAt(redoStack.lastIndex)
            bumpRecipeRevision()
        }
    }

    private fun pushToUndoStack() {
        undoStack.add(_currentOperations.value)
        redoStack.clear()
    }

    fun exportRecording() {
        val recording = uiState.value.recording ?: return
        if (_isExporting.value) return
        _shareIntent.value = null
        _lastExportedRecording.value = null
        _exportSuccessMessage.value = null
        _errorMessage.value = null
        _exportProgress.value = 0
        exportJob = viewModelScope.launch {
            _isExporting.value = true
            val result = audioEditExporter.exportEditedRecording(
                originalRecording = recording,
                recipe = uiState.value.recipe
            ) { progress ->
                _exportProgress.value = progress
            }
            _isExporting.value = false
            _exportProgress.value = null
            if (result.isSuccess) {
                val exported = result.getOrNull()
                _lastExportedRecording.value = exported
                _shareIntent.value = exported?.let { exportedRecording ->
                    recordingStorageManager.buildShareIntent(exportedRecording).getOrNull()
                }
                _exportSuccessMessage.value = "Edited audio exported as a new recording."
            } else {
                _errorMessage.value = result.exceptionOrNull().toUserMessage()
            }
        }
    }

    fun cancelExport() {
        exportJob?.cancel()
    }

    fun consumeShareIntent() {
        _shareIntent.value = null
    }

    fun shareLastExportedRecording() {
        val exportedRecording = _lastExportedRecording.value ?: return
        _shareIntent.value = recordingStorageManager.buildShareIntent(exportedRecording).getOrNull()
    }

    private fun bumpRecipeRevision() {
        _recipeRevision.value += 1L
    }

    private fun KeepRange.toEditPreviewSegment(): EditPreviewSegment {
        return EditPreviewSegment(startMs = startMs, endMs = endMs)
    }

    private fun Throwable?.toUserMessage(): String {
        return when (this) {
            AudioEditException.SourceMissing -> "The original audio file is missing."
            AudioEditException.InvalidRecipe -> "The current edit selection is invalid."
            AudioEditException.EmptyResult -> "The current edit would create an empty recording."
            AudioEditException.UnsupportedOperation -> "This edit option is not supported yet on the current device/export path."
            AudioEditException.OutputValidationFailed -> "Loomora could not validate the exported audio output."
            AudioEditException.ExportCancelled -> "Audio export was cancelled."
            else -> "Audio export failed."
        }
    }
}

private data class RecipeStateBundle(
    val recording: Recording?,
    val operations: List<EditOperation>,
    val isSpeechClarityEnabled: Boolean,
    val recipeRevision: Long
)

private data class ExportStateBundle(
    val selectionStartMs: Long,
    val selectionEndMs: Long,
    val isExporting: Boolean,
    val exportSuccessMessage: String?,
    val waveform: WaveformLoadState,
    val exportProgress: Int?,
    val lastExportedRecording: Recording?,
    val shareIntent: Intent?
)

private data class ExportUiBundle(
    val selectionStartMs: Long,
    val selectionEndMs: Long,
    val isExporting: Boolean,
    val exportSuccessMessage: String?,
    val waveform: WaveformLoadState
)

private data class ExportArtifactBundle(
    val exportProgress: Int?,
    val lastExportedRecording: Recording?,
    val shareIntent: Intent?
)
