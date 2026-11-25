package com.example.quran.audio

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.quran.db.QuranAudioTrackEntity
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class QuranRepeatMode {
    OFF,
    REPEAT_SURAH,
    REPEAT_PLAYLIST
}

data class QuranAudioPlaybackState(
    val queue: List<QuranAudioTrackEntity> = emptyList(),
    val currentIndex: Int = -1,
    val currentTrack: QuranAudioTrackEntity? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    val repeatMode: QuranRepeatMode = QuranRepeatMode.OFF,
    val shuffleEnabled: Boolean = false,
    val errorMessage: String? = null
) {
    val hasActiveTrack: Boolean get() = currentTrack != null
    val remainingMs: Long get() = (durationMs - positionMs).coerceAtLeast(0L)
}

open class QuranPlayerController(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionPollJob: Job? = null
    private var pendingQueue: Pair<List<QuranAudioTrackEntity>, Int>? = null

    private val _state = MutableStateFlow(QuranAudioPlaybackState())
    val state: StateFlow<QuranAudioPlaybackState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            syncFromPlayer()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            syncFromPlayer()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncFromPlayer()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            syncFromPlayer()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            syncFromPlayer()
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = "تعذر تشغيل الملف الصوتي. تأكد من صلاحية الملف على الجهاز."
                )
            }
        }
    }

    open fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(appContext, ComponentName(appContext, QuranPlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching { future.get() }.onSuccess { controller ->
                mediaController = controller
                controller.addListener(playerListener)
                pendingQueue?.let { (tracks, startIndex) ->
                    pendingQueue = null
                    applyQueueToController(controller, tracks, startIndex)
                } ?: syncFromPlayer()
                startPositionPolling()
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    open fun playPlaylist(tracks: List<QuranAudioTrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val safeIdx = startIndex.coerceIn(0, tracks.lastIndex)
        _state.update {
            it.copy(
                queue = tracks,
                currentIndex = safeIdx,
                currentTrack = tracks[safeIdx],
                errorMessage = null
            )
        }
        val controller = mediaController
        if (controller != null) {
            applyQueueToController(controller, tracks, safeIdx)
        } else {
            pendingQueue = tracks to safeIdx
            connect()
        }
    }

    private fun applyQueueToController(
        controller: MediaController,
        tracks: List<QuranAudioTrackEntity>,
        startIndex: Int
    ) {
        val items = tracks.map { track ->
            MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(track.uriString.toUri())
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(track.displayName)
                        .setArtist(track.reciterName.ifBlank { "Quramer" })
                        .build()
                )
                .build()
        }
        controller.setMediaItems(items, startIndex, 0L)
        controller.prepare()
        controller.play()
        syncFromPlayer()
    }

    fun togglePlayPause() {
        val controller = mediaController ?: run {
            val currentQueue = _state.value.queue
            if (currentQueue.isNotEmpty()) {
                playPlaylist(currentQueue, _state.value.currentIndex.coerceAtLeast(0))
            }
            return
        }
        if (controller.isPlaying) {
            controller.pause()
        } else {
            if (controller.playbackState == Player.STATE_ENDED) {
                controller.seekToDefaultPosition(0)
            }
            controller.play()
        }
        syncFromPlayer()
    }

    fun seekTo(positionMs: Long) {
        val controller = mediaController ?: return
        val clamped = positionMs.coerceIn(0L, controller.duration.takeIf { it > 0L } ?: Long.MAX_VALUE)
        controller.seekTo(clamped)
        _state.update { it.copy(positionMs = clamped) }
    }

    fun seekForward10s() {
        val controller = mediaController ?: return
        val target = (controller.currentPosition + 10_000L)
            .coerceAtMost(controller.duration.takeIf { it > 0L } ?: Long.MAX_VALUE)
        controller.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    fun seekBack10s() {
        val controller = mediaController ?: return
        val target = (controller.currentPosition - 10_000L).coerceAtLeast(0L)
        controller.seekTo(target)
        _state.update { it.copy(positionMs = target) }
    }

    fun nextSurah() {
        val controller = mediaController ?: return
        if (controller.hasNextMediaItem()) {
            controller.seekToNextMediaItem()
        }
    }

    fun previousSurah() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 4_000L || !controller.hasPreviousMediaItem()) {
            controller.seekTo(0L)
        } else {
            controller.seekToPreviousMediaItem()
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.5f)
        mediaController?.setPlaybackSpeed(clamped)
        _state.update { it.copy(speed = clamped) }
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            QuranRepeatMode.OFF -> QuranRepeatMode.REPEAT_SURAH
            QuranRepeatMode.REPEAT_SURAH -> QuranRepeatMode.REPEAT_PLAYLIST
            QuranRepeatMode.REPEAT_PLAYLIST -> QuranRepeatMode.OFF
        }
        val media3Repeat = when (next) {
            QuranRepeatMode.OFF -> Player.REPEAT_MODE_OFF
            QuranRepeatMode.REPEAT_SURAH -> Player.REPEAT_MODE_ONE
            QuranRepeatMode.REPEAT_PLAYLIST -> Player.REPEAT_MODE_ALL
        }
        mediaController?.repeatMode = media3Repeat
        _state.update { it.copy(repeatMode = next) }
    }

    fun toggleShuffle() {
        val next = !_state.value.shuffleEnabled
        mediaController?.shuffleModeEnabled = next
        _state.update { it.copy(shuffleEnabled = next) }
    }

    fun pauseIfPlaying() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
            syncFromPlayer()
        }
    }

    fun stopPlayback() {
        mediaController?.stop()
        _state.update {
            it.copy(
                isPlaying = false,
                positionMs = 0L
            )
        }
    }

    private fun startPositionPolling() {
        positionPollJob?.cancel()
        positionPollJob = scope.launch {
            while (isActive) {
                syncFromPlayer()
                delay(400L)
            }
        }
    }

    private fun syncFromPlayer() {
        val controller = mediaController ?: return
        val queue = _state.value.queue
        val idx = controller.currentMediaItemIndex.takeIf { it in queue.indices }
            ?: _state.value.currentIndex
        val track = queue.getOrNull(idx) ?: _state.value.currentTrack
        val dur = controller.duration.takeIf { it > 0L } ?: (track?.durationMs ?: 0L)
        val pos = controller.currentPosition.coerceAtLeast(0L)
        val repeat = when (controller.repeatMode) {
            Player.REPEAT_MODE_ONE -> QuranRepeatMode.REPEAT_SURAH
            Player.REPEAT_MODE_ALL -> QuranRepeatMode.REPEAT_PLAYLIST
            else -> QuranRepeatMode.OFF
        }
        _state.update {
            it.copy(
                currentIndex = idx,
                currentTrack = track,
                isPlaying = controller.isPlaying,
                isBuffering = controller.playbackState == Player.STATE_BUFFERING,
                positionMs = pos,
                durationMs = dur,
                speed = controller.playbackParameters.speed,
                repeatMode = repeat,
                shuffleEnabled = controller.shuffleModeEnabled
            )
        }
    }

    open fun release() {
        positionPollJob?.cancel()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }
}
