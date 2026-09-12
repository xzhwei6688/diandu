package com.example.diandu

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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

class MainActivity : AppCompatActivity() {

    private val REQUEST_PICK_IMAGE = 200

    private lateinit var ivPreview: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnTakePhoto: Button
    private lateinit var btnRecognize: Button
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivPreview = findViewById(R.id.ivPreview)
        progressBar = findViewById(R.id.progressBar)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnRecognize = findViewById(R.id.btnRecognize)

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

                if (result.isBlank()) {
                    Toast.makeText(this@MainActivity, "没识别到文字，换一张试试", Toast.LENGTH_LONG).show()
                } else {
                    val intent = Intent(this@MainActivity, ReadActivity::class.java)
                    intent.putExtra("text", result)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                btnRecognize.isEnabled = true
                btnTakePhoto.isEnabled = true
                Toast.makeText(this@MainActivity, "识别失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
