package com.urdu.dub

import android.util.Base64
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiLive(
    private val apiKey: String,
    private val targetLangCode: String,
    private val onReady: () -> Unit,
    private val onAudio: (ByteArray) -> Unit,
    private val onText: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    @Volatile private var ws: WebSocket? = null
    @Volatile private var ready = false

    fun connect() {
        val url = "wss://generativelanguage.googleapis.com/ws/" +
            "google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent" +
            "?key=$apiKey"
        val req = Request.Builder().url(url).build()

        ws = client.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send(setupJson().toString())
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                try { handle(text) } catch (e: Exception) { onError(e.message ?: "parse") }
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                ready = false
                onError(t.message ?: "websocket failed")
            }
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                ready = false
            }
        })
    }

    private fun setupJson(): JSONObject {
        val gen = JSONObject()
            .put("responseModalities", JSONArray().put("AUDIO"))
            .put("inputAudioTranscription", JSONObject())
            .put("outputAudioTranscription", JSONObject())
            .put("translationConfig", JSONObject()
                .put("targetLanguageCode", targetLangCode)
                .put("echoTargetLanguage", true))

        val setup = JSONObject()
            .put("model", "models/${Config.MODEL}")
            .put("generationConfig", gen)

        return JSONObject().put("setup", setup)
    }

    private fun handle(text: String) {
        val json = JSONObject(text)

        if (json.has("error")) {
            onError(json.optJSONObject("error")?.optString("message") ?: "error")
            return
        }

        if (json.has("setupComplete")) { ready = true; onReady(); return }

        val sc = json.optJSONObject("serverContent") ?: return

        val parts = sc.optJSONObject("modelTurn")?.optJSONArray("parts")
        if (parts != null) {
            for (i in 0 until parts.length()) {
                val inline = parts.getJSONObject(i).optJSONObject("inlineData") ?: continue
                val b64 = inline.optString("data")
                if (b64.isNotEmpty()) onAudio(Base64.decode(b64, Base64.DEFAULT))
            }
        }
        val out = sc.optJSONObject("outputTranscription")?.optString("text")
        if (!out.isNullOrEmpty()) onText(out)
    }

    fun sendAudio(pcm: ByteArray) {
        val sock = ws ?: return
        if (!ready) return
        val audio = JSONObject()
            .put("mimeType", "audio/pcm;rate=16000")
            .put("data", Base64.encodeToString(pcm, Base64.NO_WRAP))
        sock.send(JSONObject().put("realtimeInput", JSONObject().put("audio", audio)).toString())
    }

    fun close() {
        ready = false
        ws?.close(1000, "bye")
        ws = null
    }
}
