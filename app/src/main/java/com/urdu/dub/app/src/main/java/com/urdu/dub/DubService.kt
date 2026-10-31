package com.urdu.dub

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlin.concurrent.thread

class DubService : Service() {

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val EXTRA_DUCK = "duck"
        const val CHANNEL_ID = "dub"
        const val NOTIF_ID = 42
        const val SR_IN = 16000
        const val SR_OUT = 24000

        @Volatile var isRunning = false
        @Volatile var lastText = ""
        @Volatile var dubVolume = 1.0f
    }

    private var projection: MediaProjection? = null
    private var record: AudioRecord? = null
    private var track: AudioTrack? = null
    private var live: GeminiLive? = null
    private var am: AudioManager? = null
    private val focusListener = AudioManager.OnAudioFocusChangeListener { }
    @Volatile private var capturing = false
    private var savedMusicVol = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopAll(); return START_NOT_STICKY }
        startDubbing(intent)
        return START_NOT_STICKY
    }

    private fun startDubbing(intent: Intent?) {
        startForegroundCompat()

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        @Suppress("DEPRECATION")
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA) ?: return
        val duck = intent?.getBooleanExtra(EXTRA_DUCK, true) ?: true

        if (duck) {
            am?.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            if (Build.VERSION.SDK_INT >= 31) {
                runCatching {
                    am?.setStreamVolume(AudioManager.STREAM_ASSISTANT,
                        am!!.getStreamMaxVolume(AudioManager.STREAM_ASSISTANT), 0)
                    savedMusicVol = am!!.getStreamVolume(AudioManager.STREAM_MUSIC)
                    am!!.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                }
            }
        }

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = mpm.getMediaProjection(resultCode, data) ?: return
        projection = proj
        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { stopAll() }
        }, null)

        val p = getSharedPreferences("cfg", MODE_PRIVATE)
        val apiKey = p.getString("apiKey", "") ?: ""
        val lang = Config.LANGUAGES[p.getInt("langIdx", 0)]
        val code = when {
            lang.startsWith("Urdu") -> "ur"
            lang.startsWith("Hindi") -> "hi"
            lang.startsWith("Punjabi") -> "pa"
            else -> "en"
        }

        val outFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SR_OUT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val outAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        track = AudioTrack.Builder()
            .setAudioAttributes(outAttrs)
            .setAudioFormat(outFormat)
            .setBufferSizeInBytes(
                AudioTrack.getMinBufferSize(SR_OUT,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT) * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build().also { it.play() }

        live = GeminiLive(
            apiKey = apiKey,
            targetLangCode = code,
            onReady = { startCapture() },
            onAudio = { pcm ->
                track?.let { t -> t.setVolume(dubVolume); t.write(pcm, 0, pcm.size) }
            },
            onText = { lastText = Roman.toRoman(it) },
            onError = { lastText = "Error: $it" }
        ).also { it.connect() }
    }

    private fun startCapture() {
        if (capturing) return
        val proj = projection ?: return

        val capConfig = AudioPlaybackCaptureConfiguration.Builder(proj)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .build()

        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SR_IN)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        val minBuf = AudioRecord.getMinBufferSize(
            SR_IN, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)

        val rec = AudioRecord.Builder()
            .setAudioFormat(format)
            .setBufferSizeInBytes(minBuf * 2)
            .setAudioPlaybackCaptureConfig(capConfig)
            .build()
        record = rec
        rec.startRecording()
        capturing = true

        thread(name = "capture") {
            val buf = ByteArray(minBuf)
            while (capturing) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) live?.sendAudio(buf.copyOf(n))
            }
        }
    }

    private fun stopAll() {
        capturing = false
        runCatching { record?.stop() }; record?.release(); record = null
        runCatching { track?.stop() }; track?.release(); track = null
        live?.close(); live = null
        runCatching { projection?.stop() }; projection = null
        am?.abandonAudioFocus(focusListener)
        if (savedMusicVol >= 0) {
            runCatching { am?.setStreamVolume(AudioManager.STREAM_MUSIC, savedMusicVol, 0) }
            savedMusicVol = -1
        }
        isRunning = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundCompat() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_ID, "Dubbing", NotificationManager.IMPORTANCE_LOW))
        }
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("UrduDub chalu hai")
            .setContentText("Live dubbing")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIF_ID, notif)
        }
        isRunning = true
    }

    override fun onDestroy() { stopAll(); super.onDestroy() }
}
