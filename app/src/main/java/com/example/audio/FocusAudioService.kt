package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.BuiltInSound
import com.example.data.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val activeTitle: String = ""
)

class FocusAudioService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val synthesizer = FocusAudioSynthesizer()
    private var mediaPlayer: MediaPlayer? = null
    private var sessionWatchJob: Job? = null
    private var startedDuringActiveSession = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_BUILTIN -> {
                val soundId = intent.getStringExtra(EXTRA_SOUND_ID)
                val sound = BuiltInSound.fromId(soundId) ?: BuiltInSound.RAIN
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.65f)
                val loop = intent.getBooleanExtra(EXTRA_LOOP, true)
                val title = titleForBuiltIn(sound)
                startForegroundWithNotification(title)
                playBuiltIn(sound, volume, loop, title)
            }
            ACTION_PLAY_URI -> {
                val uriStr = intent.getStringExtra(EXTRA_URI)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: getString(R.string.app_name)
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.65f)
                val loop = intent.getBooleanExtra(EXTRA_LOOP, true)
                if (uriStr.isNullOrBlank()) {
                    stopPlaybackAndSelf()
                } else {
                    startForegroundWithNotification(title)
                    playUri(Uri.parse(uriStr), volume, loop, title)
                }
            }
            ACTION_UPDATE_CONTROLS -> {
                val volume = intent.getFloatExtra(EXTRA_VOLUME, 0.65f).coerceIn(0f, 1f)
                val loop = intent.getBooleanExtra(EXTRA_LOOP, true)
                synthesizer.setVolume(volume)
                synthesizer.setLoopEnabled(loop)
                mediaPlayer?.let { mp ->
                    runCatching {
                        mp.setVolume(volume, volume)
                        mp.isLooping = loop
                    }
                }
            }
            ACTION_STOP -> {
                stopPlaybackAndSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun playBuiltIn(sound: BuiltInSound, volume: Float, loop: Boolean, title: String) {
        releaseMediaPlayer()
        synthesizer.start(sound, volume, loop) {
            serviceScope.launch { stopPlaybackAndSelf() }
        }
        _playbackState.value = AudioPlaybackState(isPlaying = true, activeTitle = title)
        startSessionWatcher()
    }

    private fun playUri(uri: Uri, volume: Float, loop: Boolean, title: String) {
        synthesizer.stop()
        releaseMediaPlayer()

        val mp = MediaPlayer()
        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mp.setDataSource(applicationContext, uri)
            mp.isLooping = loop
            mp.setVolume(volume, volume)
            mp.setOnCompletionListener {
                if (!loop) {
                    stopPlaybackAndSelf()
                }
            }
            mp.setOnErrorListener { _, _, _ ->
                stopPlaybackAndSelf()
                true
            }
            mp.prepare()
            mp.start()
            mediaPlayer = mp
            _playbackState.value = AudioPlaybackState(isPlaying = true, activeTitle = title)
            startSessionWatcher()
        } catch (_: Throwable) {
            runCatching { mp.release() }
            stopPlaybackAndSelf()
        }
    }

    private fun startSessionWatcher() {
        val prefs = getSharedPreferences(FocusRepository.PREFS_NAME, MODE_PRIVATE)
        startedDuringActiveSession = prefs.getLong(FocusRepository.KEY_END, 0L) > System.currentTimeMillis()
        sessionWatchJob?.cancel()
        sessionWatchJob = serviceScope.launch {
            while (isActive) {
                val endMs = prefs.getLong(FocusRepository.KEY_END, 0L)
                val now = System.currentTimeMillis()
                if (endMs > now) {
                    startedDuringActiveSession = true
                } else if (startedDuringActiveSession && now >= endMs) {
                    stopPlaybackAndSelf()
                    break
                }
                delay(1000L)
            }
        }
    }

    private fun startForegroundWithNotification(trackTitle: String) {
        ensureNotificationChannel()
        val notification = buildNotification(trackTitle)
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val existing = nm.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.audio_notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                )
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(trackTitle: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, FocusAudioService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.audio_notification_title))
            .setContentText(getString(R.string.audio_notification_body, trackTitle))
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(0, getString(R.string.audio_notification_stop), stopIntent)
            .build()
    }

    private fun titleForBuiltIn(sound: BuiltInSound): String {
        return when (sound) {
            BuiltInSound.RAIN -> getString(R.string.audio_rain)
            BuiltInSound.WHITE_NOISE -> getString(R.string.audio_white_noise)
            BuiltInSound.ALPHA -> getString(R.string.audio_alpha)
            BuiltInSound.BETA -> getString(R.string.audio_beta)
            BuiltInSound.GAMMA -> getString(R.string.audio_gamma)
        }
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let { mp ->
            runCatching {
                if (mp.isPlaying) mp.stop()
                mp.release()
            }
        }
        mediaPlayer = null
    }

    private fun stopPlaybackAndSelf() {
        sessionWatchJob?.cancel()
        synthesizer.stop()
        releaseMediaPlayer()
        _playbackState.value = AudioPlaybackState(isPlaying = false, activeTitle = "")
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        sessionWatchJob?.cancel()
        synthesizer.stop()
        releaseMediaPlayer()
        _playbackState.value = AudioPlaybackState(isPlaying = false, activeTitle = "")
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "focus_audio_channel"
        private const val NOTIFICATION_ID = 4102

        const val ACTION_PLAY_BUILTIN = "com.example.audio.PLAY_BUILTIN"
        const val ACTION_PLAY_URI = "com.example.audio.PLAY_URI"
        const val ACTION_UPDATE_CONTROLS = "com.example.audio.UPDATE_CONTROLS"
        const val ACTION_STOP = "com.example.audio.STOP"

        const val EXTRA_SOUND_ID = "sound_id"
        const val EXTRA_URI = "uri"
        const val EXTRA_TITLE = "title"
        const val EXTRA_VOLUME = "volume"
        const val EXTRA_LOOP = "loop"

        private val _playbackState = MutableStateFlow(AudioPlaybackState())
        val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

        fun startBuiltIn(context: Context, sound: BuiltInSound, volume: Float, loop: Boolean) {
            val intent = Intent(context, FocusAudioService::class.java).apply {
                action = ACTION_PLAY_BUILTIN
                putExtra(EXTRA_SOUND_ID, sound.id)
                putExtra(EXTRA_VOLUME, volume)
                putExtra(EXTRA_LOOP, loop)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun startUploadedUri(
            context: Context,
            uriString: String,
            title: String,
            volume: Float,
            loop: Boolean
        ) {
            val intent = Intent(context, FocusAudioService::class.java).apply {
                action = ACTION_PLAY_URI
                putExtra(EXTRA_URI, uriString)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_VOLUME, volume)
                putExtra(EXTRA_LOOP, loop)
            }
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun updateControls(context: Context, volume: Float, loop: Boolean) {
            if (!_playbackState.value.isPlaying) return
            val intent = Intent(context, FocusAudioService::class.java).apply {
                action = ACTION_UPDATE_CONTROLS
                putExtra(EXTRA_VOLUME, volume)
                putExtra(EXTRA_LOOP, loop)
            }
            context.startService(intent)
        }

        fun stop(context: Context) {
            if (!_playbackState.value.isPlaying) return
            val intent = Intent(context, FocusAudioService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
