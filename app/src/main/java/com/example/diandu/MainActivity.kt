package com.example.diandu

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val REQUEST_PICK_IMAGE = 200

    private lateinit var ivPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnTakePhoto: Button
    private lateinit var btnRecognize: Button
    private var currentBitmap: Bitmap? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivPreview = findViewById(R.id.ivPreview)
        progressBar = findViewById(R.id.progressBar)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnRecognize = findViewById(R.id.btnRecognize)

        // 初始化 TTS
        tts = TextToSpeech(this, this)

        btnTakePhoto.setOnClickListener { pickImage() }

        btnRecognize.setOnClickListener {
            val bmp = currentBitmap
            if (bmp == null) {
                Toast.makeText(this, "请先选照片", Toast.LENGTH_SHORT).show()
            } else {
                recognizeText(bmp)
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                try {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    val bmp = BitmapFactory.decodeStream(inputStream)
                    currentBitmap = bmp
                    ivPreview.setImageBitmap(bmp)
                    Toast.makeText(this, "照片已选择，点\"识别文字\"", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "读取图片失败", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun recognizeText(bitmap: Bitmap) {
        progressBar.visibility = View.VISIBLE
        btnRecognize.isEnabled = false
        btnTakePhoto.isEnabled = false

        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val dataDir = File(filesDir, "tesseract")
                    val tessDataDir = File(dataDir, "tessdata")
                    if (!tessDataDir.exists()) tessDataDir.mkdirs()

                    val trainedFile = File(tessDataDir, "eng.traineddata")
                    if (!trainedFile.exists()) {
                        assets.open("tessdata/eng.traineddata").use { input ->
                            FileOutputStream(trainedFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }

                    val tessApi = TessBaseAPI()
                    tessApi.init(dataDir.absolutePath, "eng")
                    tessApi.setImage(bitmap)
                    val text = tessApi.utF8Text ?: ""
                    tessApi.recycle()
                    text
                }

                progressBar.visibility = View.GONE
                btnRecognize.isEnabled = true
                btnTakePhoto.isEnabled = true

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
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnRecognize.isEnabled = true
                btnTakePhoto.isEnabled = true
                Toast.makeText(this@MainActivity, "识别失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun speak(text: String) {
        if (!ttsReady || tts == null) {
            Toast.makeText(this, "语音引擎未就绪，请去系统设置安装TTS", Toast.LENGTH_LONG).show()
            return
        }
        // 关键修复：强制使用媒体音量，防止被静音
        val params = Bundle()
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "diandu")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            // 关键修复：先试美式英语，不行就试通用英语
            var result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                result = tts?.setLanguage(Locale.ENGLISH)
            }
            
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsReady = false
                Toast.makeText(this, "系统缺少英语语音包，请安装 TTS 引擎", Toast.LENGTH_LONG).show()
            } else {
                ttsReady = true
            }
        } else {
            ttsReady = false
            Toast.makeText(this, "TTS 初始化失败，请安装语音引擎", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
