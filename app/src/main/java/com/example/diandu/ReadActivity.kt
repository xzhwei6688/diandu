package com.example.diandu

import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ReadActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read)

        container = findViewById(R.id.containerSentences)
        tts = TextToSpeech(this, this)

        // 接收上一页传过来的文字
        val text = intent.getStringExtra("text") ?: ""

        if (text.isNotBlank()) {
            // 按照 . ! ? 切分成句子
            val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            for (sentence in sentences) {
                if (sentence.isBlank()) continue

                // 动态生成一个个可点击的句子
                val tv = TextView(this)
                tv.text = sentence
                tv.textSize = 20f
                tv.setPadding(0, 16, 0, 16)
                tv.gravity = Gravity.START
                tv.setOnClickListener {
                    speak(sentence)
                    Toast.makeText(this, "正在朗读本句...", Toast.LENGTH_SHORT).show()
                }
                container.addView(tv)
            }
        }
    }

    private fun speak(text: String) {
        if (!ttsReady || tts == null) {
            Toast.makeText(this, "语音引擎未就绪", Toast.LENGTH_SHORT).show()
            return
        }
        val params = Bundle()
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "read")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            var result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = tts?.setLanguage(Locale.ENGLISH)
            }
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
