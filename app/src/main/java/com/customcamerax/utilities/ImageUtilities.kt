package com.customcamerax.utilities

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.core.net.toUri
import com.customcamerax.CameraActivity
import com.customcamerax.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class ImageUtilities {
    fun viewToBitmap(imageView: ImageView): Bitmap {
        return (imageView.drawable as BitmapDrawable).bitmap
    }

    fun getResizedBitmap(image: Bitmap, maxSize: Int): Bitmap {
        var width = image.width
        var height = image.height
        val bitmapRatio = width.toFloat() / height.toFloat()
        if (bitmapRatio > 1) {
            width = maxSize
            height = (width / bitmapRatio).toInt()
        } else {
            height = maxSize
            width = (height * bitmapRatio).toInt()
        }
        return Bitmap.createScaledBitmap(image, width, height, true)
    }

    fun saveImage(image: Any, activity: Activity, typeProcess: String) { //bitmap: Bitmap imageView: ImageView
        val fileName: String
        var typeName = ""
        val bitmap: Any

        if (image is ImageView) {
            bitmap = viewToBitmap(image)
        } else {
            bitmap = image as Bitmap
        }
//        val bitmap: Bitmap = viewToBitmap(imageView)
        val quality = 50 // default 100

        when (typeProcess) {
            "Crop" -> typeName = "crop"
            "Resize" -> typeName = "resize"
            else -> {}
        }

        fileName = "${typeName}_${
            SimpleDateFormat(CameraActivity.FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())}.jpg"

/*        ByteArrayOutputStream().apply {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, this)
            fileName = "${typeName}_${
                SimpleDateFormat(CameraActivity.FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis())}.jpg"
        }*/

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
            val imageFile =  File("${activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)}" +
                    "/${activity.resources.getString(R.string.app_name)}"
            )

            if (imageFile.exists()) {
                Toast.makeText(activity, "Already saved", Toast.LENGTH_SHORT).show()
            } else {
                val contentResolver = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/${activity.resources.getString(
                        R.string.app_name
                    )}")
                }

                activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentResolver).apply {
                    this?.let { uri ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, activity.contentResolver.openOutputStream(uri))
                    }
                }
                val uriPath = imageFile.toURI()

                Toast.makeText(activity, "saved to $uriPath", Toast.LENGTH_SHORT).show()
            }
        } else {
//            val pathDir = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES).toString()
//            val imageDir = File("$pathDir/${resources.getString(R.string.app_name)}")
            val imageDir = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}")
            imageDir.mkdirs()
            val imageFile = File(imageDir, fileName)
            val fos: OutputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
            val uriPath = imageFile.toURI()
            val updateGallery = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            updateGallery.data = imageFile.toUri()
            activity.sendBroadcast(updateGallery)
            Toast.makeText(activity, "saved to $uriPath", Toast.LENGTH_SHORT).show()
        }
    }

}