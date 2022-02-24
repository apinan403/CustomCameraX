package com.customcamerax

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.customcamerax.databinding.ActivityMainBinding
import androidx.core.app.ActivityCompat.startActivityForResult
import android.R
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View

import android.widget.TextView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.BitmapFactory
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {
    lateinit var bindingmainActivity: ActivityMainBinding
    private val CAMERA_ACTIVITY_REQUEST_CODE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bindingmainActivity = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindingmainActivity.root)

        bindingmainActivity.activityMainIVShowImage.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivityForResult(intent, CAMERA_ACTIVITY_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // check that it is the SecondActivity with an OK result
        if (resultCode == RESULT_OK && requestCode == CAMERA_ACTIVITY_REQUEST_CODE) {
            val uri = Uri.parse(data?.getStringExtra("picturePath"))
            Log.d("CameraXApp", "Uri: $uri")
            bindingmainActivity.activityMainIVShowImage.setImageURI(uri)
        }
    }
}

//            val byteArray = intent.getByteArrayExtra("picturePath")
//            var bitmap = byteArray?.size?.let { BitmapFactory.decodeByteArray(byteArray, 0, it) }
//            val bitmap = data?.getParcelableExtra<ByteArray>("picturePath")
//            val uri: Uri? = getImageUri(applicationContext, bitmap) // GET URI FROM THE BITMAP
//            Toast.makeText(baseContext, "Get Image From $uri", Toast.LENGTH_SHORT).show()
//            val imageProxy = data?.getStringExtra("picturePath")
//            val bitmap: Bitmap = imageProxyToBitmap(imageProxy)

/*
private fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
    val bytes = ByteArrayOutputStream()
    inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(),
        inImage,
        "IMG"+ SimpleDateFormat(
            "yyyyMMddHHmm", Locale.US).format(System.currentTimeMillis()),
        null)
    return Uri.parse(path)
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
    val planeProxy = image.planes[0]
    val buffer: ByteBuffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}*/
