package com.example.diandu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val REQUEST_CAMERA = 100
    private val REQUEST_PERMISSION = 101
    private var photoFile: File? = null

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

        tts = TextToSpeech(this, this)

        btnTakePhoto.setOnClickListener { checkPermissionAndTakePhoto() }

        btnRecognize.setOnClickListener {
            val bmp = currentBitmap
            if (bmp == null) {
                Toast.makeText(this, "请先拍照", Toast.LENGTH_SHORT).show()
            } else {
                recognizeText(bmp)
            }
        }
    }

    private fun checkPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_PERMISSION
            )
        } else {
            takePhoto()
        }
    }

    private fun takePhoto() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val dir = File(filesDir, "images")
            if (!dir.exists()) dir.mkdirs()
            photoFile = File(dir, "book_${System.currentTimeMillis()}.jpg")
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                photoFile!!
            )
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivityForResult(intent, REQUEST_CAMERA)
        } else {
            Toast.makeText(this, "没有找到相机应用", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == RESULT_OK) {
            photoFile?.let {
                val bmp = BitmapFactory.decodeFile(it.absolutePath)
                currentBitmap = bmp
                ivPreview.setImageBitmap(bmp)
                Toast.makeText(this, "拍照成功，点\"识别文字\"", Toast.LENGTH_SHORT).show()
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

                if (result.isBlank()) {
                    Toast.makeText(this@MainActivity, "没识别到文字，试试重拍", Toast.LENGTH_LONG).show()
                } else {
                    speak(result)
                    Toast.makeText(this@MainActivity, "识别成功，正在朗读！", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "语音引擎未就绪，请去系统设置下载英文语音包", Toast.LENGTH_LONG).show()
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "diandu")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto()
            } else {
                Toast.makeText(this, "需要相机权限才能拍照", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
