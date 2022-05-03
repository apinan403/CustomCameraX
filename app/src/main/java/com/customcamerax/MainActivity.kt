package com.customcamerax

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageView
import com.canhub.cropper.options
import com.customcamerax.databinding.ActivityMainBinding
import com.customcamerax.utilities.ImageUtilities
import com.customcamerax.utilities.stringResToast
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.destination
import id.zelory.compressor.constraint.quality
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(){
    private lateinit var viewBindingMainActivity: ActivityMainBinding
    private val imageUtilities = ImageUtilities()

    private val startIntentActivityCamera = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {result ->
        if (result.resultCode == RESULT_OK) {
            val imageUri = Uri.parse(result.data?.getStringExtra("picturePath"))
            Log.d(CameraActivity.TAG, "Uri: $imageUri")

            val uri = Uri.parse(getRealPath(this, imageUri))
            val file = File(uri.path.toString())
            Log.d(CameraActivity.TAG, "Picture Path: ${uri.path.toString()}")

/*            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                resizeImage(ImageDecoder.decodeBitmap(ImageDecoder.createSource(applicationContext.contentResolver, imageUri)))
            } else {
                resizeImage(MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, imageUri))
            }*/

            viewBindingMainActivity.imageViewShowImage.setImageURI(imageUri)
            viewBindingMainActivity.buttonCapture.isVisible = false

            // Normal ขนาดไฟล์เล็กกว่า คุณภาพของรูปด้อยกว่านิดหน่อย dimention ไม่เหมือนเดิม
            resizeImage(imageUtilities.viewToBitmap(viewBindingMainActivity.imageViewShowImage))

            // Use library ขนาดไฟล์ใหญ่กว่า คุณภาพของรูปดีกว่านิดหน่อย dimention เหมือนเดิม
//            compressImage(FileUtil.from(this, imageUri))

            cropImage(imageUri)
            file.deleteRecursively()
            Log.d(CameraActivity.TAG, "delete file " + file.path)
        }
    }

    private val cropImageActivityResult = registerForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            val imageUri: Uri? = result.uriContent

            viewBindingMainActivity.imageViewShowImage.setImageURI(imageUri)
            imageUtilities.saveImage(viewBindingMainActivity.imageViewShowImage, this, "Crop")
        } else {
            val exception = result.error
            Log.e(CameraActivity.TAG, "Exception: $exception")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBindingMainActivity = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBindingMainActivity.root)

        viewBindingMainActivity.buttonCapture.setOnClickListener {
            val openCameraActivity = Intent(this, CameraActivity::class.java)
            startIntentActivityCamera.launch(openCameraActivity)
        }

        viewBindingMainActivity.imageViewShowImage.setOnClickListener {
            val openCameraActivity = Intent(this, CameraActivity::class.java)
            startIntentActivityCamera.launch(openCameraActivity)
        }
    }

    private fun resizeImage(bitmap: Bitmap) {
        viewBindingMainActivity.buttonResizeImg.isVisible = true
        viewBindingMainActivity.buttonResizeImg.setOnClickListener {
            // resize with reduce dimention
            val size = 1250
            viewBindingMainActivity.imageViewShowImage.setImageBitmap(imageUtilities.getResizedBitmap(bitmap, size))
            imageUtilities.saveImage(imageUtilities.getResizedBitmap(bitmap, size), this, "Resize")

            // resize with reduce quality
/*            viewBindingMainActivity.imageViewShowImage.setImageBitmap(bitmap)
            imageUtilities.saveImage(viewBindingMainActivity.imageViewShowImage, this, "Resize")*/
//            val resizedImage = Bitmap.createScaledBitmap(bitmap, 500, 600, true)
        }
    }

    private fun cropImage(uri: Uri) {
        viewBindingMainActivity.buttonCropImg.isVisible = true
        viewBindingMainActivity.buttonCropImg.setOnClickListener{
            cropImageActivityResult.launch(
                options(uri) {
                    setGuidelines(CropImageView.Guidelines.ON)
                    setAspectRatio(1920, 1080)
                    setCropShape(CropImageView.CropShape.RECTANGLE)
//                    setOutputCompressFormat(Bitmap.CompressFormat.PNG)
                }
            )
        }
    }

    private fun compressImage(file: File) {
        viewBindingMainActivity.buttonResizeImg.isVisible = true
        viewBindingMainActivity.buttonResizeImg.setOnClickListener {
            file.let {
                lifecycleScope.launch {
                    val compressedImageFile: File = Compressor.compress(this@MainActivity, it) {
                        val fileName = "resize_${SimpleDateFormat(CameraActivity.FILENAME_FORMAT, Locale.US)
                                .format(System.currentTimeMillis())}.jpg"

                        val imageFile = File("${Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES)}" +
                                "/${this@MainActivity.resources.getString(R.string.app_name)}/$fileName")

                        quality(20) //ไฟล์รูปที่ได้จะชัดกว่า แต่ถ้าไฟล์เดิมขนาดใหญ่มาก (6|7 MB+) ขนาดไฟล์น่าจะลดลงไปเหลือประมาณ 1MB++
//                        size(1_048_576) //limit less size 1 MB // ยิ่งขนาดเล็ก noise ยิ่งเห็นชัด เพราะการทำงานเบื้องหลังเป็นการลด quality
                        destination(imageFile)
                    }
                    stringResToast(message = "Compressed image save in ${it.path}")
                    Log.d(CameraActivity.TAG, "Compressed image save in ${it.path}")
                    setCompressedImage(it)

                    val updateGallery = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    updateGallery.data = compressedImageFile.toUri()
                    this@MainActivity.sendBroadcast(updateGallery)
                }
            }
        }
    }

    private fun setCompressedImage(file: File) {
        file.let {
            viewBindingMainActivity.imageViewShowImage.setImageBitmap(BitmapFactory.decodeFile(it.absolutePath))

            if (it.exists()) {
                if (it.deleteRecursively()) {
                    Log.d(CameraActivity.TAG, "delete file " + it.path)
                }
            }
//            val lengthFile: Long = it.length()/1024
//            Log.d(CameraActivity.TAG,  "ขนาดไฟล์ $lengthFile KB")
//            imageUtilities.saveImage(viewBindingMainActivity.imageViewShowImage, this, "Resize")
        }
    }

    private fun getRealPath(context: Context, uri: Uri): String? {
        val contentResolver: ContentResolver = context.contentResolver ?: return null

        // Create file path inside app's data dir
        val filePath: String = (context.applicationInfo.dataDir.toString() + File.separator
                + System.currentTimeMillis())
        val file = File(filePath)
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val outputStream: OutputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream.read(buf).also { len = it } > 0) outputStream.write(buf, 0, len)
            outputStream.close()
            inputStream.close()
        } catch (ignore: IOException) {
            return null
        }
        return file.absolutePath
    }
}

/*            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source: ImageDecoder.Source = ImageDecoder.createSource(applicationContext.contentResolver, imageUri)
                val bitmap = ImageDecoder.decodeBitmap(source)
                resizeImage(bitmap)
            } else {
                val bitmap = MediaStore.Images.Media.getBitmap(applicationContext.contentResolver, imageUri)
                resizeImage(bitmap)
            }*/

/*
val lengthFile: Long = file.length()/1024
Log.d("CameraXApp",  "ขนาดไฟล์ ${ lengthFile } KB")*/
