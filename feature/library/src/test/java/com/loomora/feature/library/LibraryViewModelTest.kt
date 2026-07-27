package com.loomora.feature.library

import androidx.test.core.app.ApplicationProvider
import com.loomora.core.audio.storage.RecordingStorageManager
import com.loomora.core.model.Recording
import com.loomora.core.model.RecordingOperationResult
import com.loomora.core.model.StorageUsageSummary
import com.loomora.core.model.repository.RecordingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
class FakeRecordingRepository : RecordingRepository {
    val recordingsFlow = MutableStateFlow<List<Recording>>(emptyList())
    val recoveryDiagnosticsFlow = MutableStateFlow<List<Recording>>(emptyList())
    val trashedRecordingsFlow = MutableStateFlow<List<Recording>>(emptyList())
    val deletedIds = mutableListOf<String>()
    val restoredIds = mutableListOf<String>()

    override fun getActiveRecordings(): Flow<List<Recording>> = recordingsFlow
    override fun getRecoveryDiagnostics(): Flow<List<Recording>> = recoveryDiagnosticsFlow
    override fun getFavoriteRecordings(): Flow<List<Recording>> = recordingsFlow
    override fun getTrashedRecordings(): Flow<List<Recording>> = trashedRecordingsFlow
    override fun getRecordingById(id: String): Flow<Recording?> = MutableStateFlow(null)
    override fun searchRecordings(query: String): Flow<List<Recording>> = recordingsFlow
    override suspend fun insertRecording(recording: Recording) {}
    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {}
    override suspend fun renameRecording(id: String, newTitle: String): RecordingOperationResult = RecordingOperationResult.Success
    override suspend fun softDeleteRecording(id: String): RecordingOperationResult = RecordingOperationResult.Success
    override suspend fun restoreRecording(id: String): RecordingOperationResult {
        restoredIds += id
        return RecordingOperationResult.Success
    }
    override suspend fun deleteRecordingPermanently(id: String): RecordingOperationResult {
        deletedIds += id
        return RecordingOperationResult.Success
    }
}

private class FakeRecordingStorageManager : RecordingStorageManager(
    ApplicationProvider.getApplicationContext()
) {
    var freeBytes = 512L * 1024L * 1024L
    override fun getStorageUsageSummary(): StorageUsageSummary = StorageUsageSummary(
        recordingsBytes = 10L,
        exportsBytes = 20L,
        tempBytes = 30L,
        modelsBytes = 40L,
        freeBytes = freeBytes
    )
    override fun availableBytes(): Long = freeBytes
}

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class LibraryViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun libraryUiState_defaultValuesAreValid() = runTest(dispatcher) {
        val fakeRepo = FakeRecordingRepository()
        val viewModel = LibraryViewModel(fakeRepo, FakeRecordingStorageManager())
        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals("", state.searchQuery)
        assertEquals(SortOption.NEWEST, state.sortOption)
        assertEquals(false, state.showFavoritesOnly)
        assertEquals(false, state.showTrashOnly)
        assertTrue(state.recoveryDiagnostics.isEmpty())
        assertEquals(10L, state.storageUsage.recordingsBytes)
        collectionJob.cancel()
    }

    @Test
    fun recoveryDiagnostics_areExposedSeparatelyFromSavedRecordings() = runTest(dispatcher) {
        val fakeRepo = FakeRecordingRepository()
        fakeRepo.recordingsFlow.value = listOf(recording(id = "saved"))
        fakeRepo.recoveryDiagnosticsFlow.value = listOf(
            recording(id = "missing", recoveryState = "MISSING_FILE")
        )

        val viewModel = LibraryViewModel(fakeRepo, FakeRecordingStorageManager())
        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.recordings.size)
        assertEquals(1, state.recoveryDiagnostics.size)
        assertEquals("missing", state.recoveryDiagnostics.first().id)
        collectionJob.cancel()
    }

    @Test
    fun keepRecoveryDiagnostic_hidesDiagnosticWithoutDeletingFile() = runTest(dispatcher) {
        val fakeRepo = FakeRecordingRepository()
        fakeRepo.recoveryDiagnosticsFlow.value = listOf(
            recording(id = "keep-me", recoveryState = "CORRUPT_FILE")
        )

        val viewModel = LibraryViewModel(fakeRepo, FakeRecordingStorageManager())
        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.recoveryDiagnostics.size)
        viewModel.keepRecoveryDiagnostic("keep-me")
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeRepo.deletedIds.isEmpty())
        assertTrue(viewModel.uiState.value.recoveryDiagnostics.isEmpty())
        collectionJob.cancel()
    }

    @Test
    fun deleteRecoveryDiagnostic_deletesAndRemovesDiagnosticFromUiState() = runTest(dispatcher) {
        val fakeRepo = FakeRecordingRepository()
        fakeRepo.recoveryDiagnosticsFlow.value = listOf(
            recording(id = "delete-me", recoveryState = "ZERO_BYTE_FILE")
        )

        val viewModel = LibraryViewModel(fakeRepo, FakeRecordingStorageManager())
        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.recoveryDiagnostics.size)
        viewModel.deleteRecoveryDiagnostic("delete-me")
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("delete-me"), fakeRepo.deletedIds)
        assertFalse(viewModel.uiState.value.recoveryDiagnostics.any { it.id == "delete-me" })
        collectionJob.cancel()
    }

    @Test
    fun toggleTrashOnly_switchesToTrashFlowAndRestoreUsesRepository() = runTest(dispatcher) {
        val fakeRepo = FakeRecordingRepository()
        fakeRepo.trashedRecordingsFlow.value = listOf(recording(id = "trash-1"))
        val viewModel = LibraryViewModel(fakeRepo, FakeRecordingStorageManager())

        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.toggleTrashOnly()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showTrashOnly)
        assertEquals(1, viewModel.uiState.value.trashedRecordings.size)

        viewModel.restoreRecording("trash-1")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(listOf("trash-1"), fakeRepo.restoredIds)
        collectionJob.cancel()
    }

    @Test
    fun lowStorageWarning_isExposedWhenFreeSpaceDropsBelowThreshold() = runTest(dispatcher) {
        val fakeRepo = FakeRecordingRepository()
        val storageManager = FakeRecordingStorageManager().apply {
            freeBytes = 64L * 1024L * 1024L
        }
        val viewModel = LibraryViewModel(fakeRepo, storageManager)

        val collectionJob = backgroundScope.launch { viewModel.uiState.collect {} }
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.lowStorageWarning)
        collectionJob.cancel()
    }

    private fun recording(
        id: String,
        recoveryState: String = "NORMAL"
    ) = Recording(
        id = id,
        title = "Recording $id",
        createdAt = 1_000L,
        updatedAt = 1_000L,
        durationMs = 30_000L,
        originalFileUri = "file:///tmp/$id.m4a",
        recoveryState = recoveryState
    )
}
