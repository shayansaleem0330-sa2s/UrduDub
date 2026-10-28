package com.urdu.dub

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var apiKey: EditText
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var duck: CheckBox
    private lateinit var button: Button
    private lateinit var langSpin: Spinner
    private lateinit var voiceSpin: Spinner
    private val prefs by lazy { getSharedPreferences("cfg", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiKey = findViewById(R.id.apiKey)
        status = findViewById(R.id.status)
        transcript = findViewById(R.id.transcript)
        duck = findViewById(R.id.duck)
        button = findViewById(R.id.button)
        langSpin = findViewById(R.id.langSpin)
        voiceSpin = findViewById(R.id.voiceSpin)
        val vol = findViewById<SeekBar>(R.id.dubVol)

        setupSpinner(langSpin, Config.LANGUAGES, "langIdx")
        setupSpinner(voiceSpin, Config.VOICES, "voiceIdx")

        apiKey.setText(prefs.getString("apiKey", ""))

        vol.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                DubService.dubVolume = p / 100f
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        button.setOnClickListener {
            if (DubService.isRunning) {
                startService(Intent(this, DubService::class.java)
                    .setAction(DubService.ACTION_STOP))
                status.text = "Band hai"
                button.text = "Dubbing shuru karo"
            } else {
                prefs.edit()
                    .putString("apiKey", apiKey.text.toString().trim())
                    .putInt("langIdx", langSpin.selectedItemPosition)
                    .putInt("voiceIdx", voiceSpin.selectedItemPosition)
                    .apply()
                if (checkPerms()) askProjection()
            }
        }

        Thread {
            while (true) {
                runOnUiThread { transcript.text = DubService.lastText }
                Thread.sleep(600)
            }
        }.start()
    }

    private fun setupSpinner(sp: Spinner, items: List<String>, prefKey: String) {
        val ad = ArrayAdapter(this, R.layout.item_spinner, items)
        ad.setDropDownViewResource(R.layout.item_spinner)
        sp.adapter = ad
        sp.setSelection(prefs.getInt(prefKey, 0))
    }

    private fun checkPerms(): Boolean {
        val need = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isEmpty()) return true
        ActivityCompat.requestPermissions(this, need.toTypedArray(), 10)
        return false
    }

    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(c, p, r)
        if (c == 10 && checkPerms()) askProjection()
    }

    private fun askProjection() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        @Suppress("DEPRECATION")
        startActivityForResult(mpm.createScreenCaptureIntent(), 99)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == 99 && res == RESULT_OK && data != null) {
            startForegroundService(Intent(this, DubService::class.java)
                .setAction(DubService.ACTION_START)
                .putExtra(DubService.EXTRA_RESULT_CODE, res)
                .putExtra(DubService.EXTRA_DATA, data)
                .putExtra(DubService.EXTRA_DUCK, duck.isChecked))
            status.text = "Chalu hai — YouTube kholo"
            button.text = "Rok do"
        }
    }
}
