package com.loomora.core.audio.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.loomora.core.audio.model.PlayerState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioPlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentRecordingId = MutableStateFlow<String?>(null)
    val currentRecordingId: StateFlow<String?> = _currentRecordingId.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var updateJob: Job? = null
    private val playerScope = CoroutineScope(Dispatchers.Main + Job())

    private var currentSpeed: Float = 1.0f

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(context).build().also { player ->
            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_BUFFERING -> _playerState.value = PlayerState.Buffering
                        Player.STATE_ENDED -> {
                            val duration = player.duration.coerceAtLeast(0L)
                            _playerState.value = PlayerState.Paused(duration, duration, currentSpeed)
                        }
                        else -> {}
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    val position = player.currentPosition.coerceAtLeast(0L)
                    val duration = player.duration.coerceAtLeast(0L)
                    if (isPlaying) {
                        _playerState.value = PlayerState.Playing(position, duration, currentSpeed)
                    } else if (player.playbackState != Player.STATE_ENDED) {
                        _playerState.value = PlayerState.Paused(position, duration, currentSpeed)
                    }
                }
            })
            exoPlayer = player
        }
    }

    fun playAudio(recordingId: String, fileUriString: String) {
        val filePath = fileUriString.removePrefix("file://")
        val file = File(filePath)

        if (!file.exists() || !file.isFile || file.length() == 0L) {
            _playerState.value = PlayerState.Error("Audio file is missing or corrupted: $filePath")
            return
        }

        _currentRecordingId.value = recordingId
        val player = getOrCreatePlayer()

        val mediaItem = MediaItem.fromUri(fileUriString)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playbackParameters = PlaybackParameters(currentSpeed)
        player.play()

        startProgressUpdates()
    }

    fun pause() {
        exoPlayer?.pause()
    }

    fun resume() {
        exoPlayer?.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.let { player ->
            player.seekTo(positionMs.coerceIn(0L, player.duration.coerceAtLeast(0L)))
            val duration = player.duration.coerceAtLeast(0L)
            if (player.isPlaying) {
                _playerState.value = PlayerState.Playing(positionMs, duration, currentSpeed)
            } else {
                _playerState.value = PlayerState.Paused(positionMs, duration, currentSpeed)
            }
        }
    }

    fun seekForward(ms: Long = 10000L) {
        exoPlayer?.let { player ->
            seekTo(player.currentPosition + ms)
        }
    }

    fun seekRewind(ms: Long = 10000L) {
        exoPlayer?.let { player ->
            seekTo(player.currentPosition - ms)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        currentSpeed = speed
        exoPlayer?.let { player ->
            player.playbackParameters = PlaybackParameters(speed)
        }
    }

    fun stop() {
        updateJob?.cancel()
        exoPlayer?.stop()
        _playerState.value = PlayerState.Idle
        _currentRecordingId.value = null
    }

    private fun startProgressUpdates() {
        updateJob?.cancel()
        updateJob = playerScope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition.coerceAtLeast(0L)
                        val dur = player.duration.coerceAtLeast(0L)
                        _playerState.value = PlayerState.Playing(pos, dur, currentSpeed)
                    }
                }
                delay(200)
            }
        }
    }
}
