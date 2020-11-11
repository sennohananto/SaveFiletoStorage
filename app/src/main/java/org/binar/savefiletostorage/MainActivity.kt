package org.binar.savefiletostorage

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private val permissionList = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    private lateinit var imageFile: File
    private val imageFileName = "MyPicture.jpg"
    private var bitmap:Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(isAllPermissionGranted()){
            requestPermissions()
        }

        btnCamera.setOnClickListener {
            takePicture()
        }

        btnSecondActivity.setOnClickListener {
            val goToSecondActivity = Intent(this, SecondActivity::class.java)
            startActivity(goToSecondActivity)
        }
    }

    private fun isAllPermissionGranted(): Boolean {
        var isDenied = false
        for(permission in permissionList){
            if(ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED)
                isDenied = true
                break
        }
        return isDenied
    }

    private fun requestPermissions(){
        ActivityCompat.requestPermissions(this, permissionList, 200)
    }

    private fun takePicture(){
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                // only for jelly bean and lower version
                startActivityForResult(
                    takePictureIntent,
                    200
                )
            } else {

//                Uri fileUri = null;
                imageFile = File(externalCacheDir, imageFileName)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, getFileUri(imageFile))


                startActivityForResult(
                    takePictureIntent,
                    200
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                200 -> onCaptureImageResult(data)
//                201 -> onSelectFromGalleryResult(data)
            }
        }
    }

    private fun onCaptureImageResult(data: Intent?) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            if (data == null) {
                Toast.makeText(
                    this,
                    "Telah terjadi kesalahan. Silakan coba beberapa saat lagi.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            if (data.extras == null) {
                Toast.makeText(
                    this,
                    "Telah terjadi kesalahan. Silakan coba beberapa saat lagi.",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            val photo = data.extras!!["data"] as Bitmap?
            if (photo != null) {
                // CALL THIS METHOD TO GET THE URI FROM THE BITMAP CAMERA JELLY BEAN
                val tempUri: Uri? = getImageUriForJellyBean(this, photo)
                bitmap = photo
                if (tempUri != null) {
                    getRealPathFromURI(tempUri, this)?.let {
                        imageFile = File(it)
                    }

                    photo.recycle()
                } else {
                    Toast.makeText(
                        this,
                        "Telah terjadi kesalahan. Silakan coba beberapa saat lagi.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Telah terjadi kesalahan. Silakan coba beberapa saat lagi.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        if (imageFile != null) {
//            ImageCompression.compressImage(profpicFile.getPath())
            val fileSize = imageFile.length() / 1024
            if (fileSize > 99000) {
                Toast.makeText(
                    this,
                    "File tidak boleh melebihi 99 MB",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            if (imageFile != null) {
                Glide
                    .with(this)
                    .asBitmap()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .load(imageFile)
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivPicture)
//                saveFile(imageFile)

                bitmap?.let {
                    saveImage(it)
                }
            }
        } else {
            Toast.makeText(
                this,
                "Telah terjadi kesalahan. Silakan coba beberapa saat lagi.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun getImageUriForJellyBean(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 65, bytes)
        val path =
            MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "capture", null)
                ?: return null
        return Uri.parse(path)
    }

    private fun getRealPathFromURI(contentURI: Uri?, context: Activity): String? {
        var res: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(contentURI!!, projection, null, null, null)
            ?: return null
        if (cursor.moveToFirst()) {
            val column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            res = cursor.getString(column_index)
        }
        cursor.close()
        return res
    }

    private fun getFileUri(file: File): Uri? {
        var file: File? = file
        var fileUri: Uri? = null
        file = File(
            this.getExternalCacheDir(),
            imageFileName
        )

        try {
            fileUri = FileProvider.getUriForFile(
                this, BuildConfig.APPLICATION_ID.toString() + ".provider",
                file!!
            )
        } catch (e: IllegalArgumentException) {
            val builder = VmPolicy.Builder()
            StrictMode.setVmPolicy(builder.build())
            fileUri = Uri.fromFile(file)
            e.printStackTrace()
        }
        return fileUri
    }

    private fun saveFile(file: File){
        if (file.exists())
            file.delete()
        try {
            val out = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveImage(finalBitmap: Bitmap) {
        val root: String = externalCacheDir.toString()
        val myDir = File(root)
        if (!myDir.exists()) {
            myDir.mkdirs()
        }

        val fname = "MyPicture.jpg"


        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}