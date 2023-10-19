package com.example.myapplication


import android.content.pm.PackageManager
import android.hardware.Camera
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var surfaceHolder: SurfaceHolder
    private var camera: Camera? = null
    private var isFrontCamera = false
    private var isFlashOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)


        surfaceHolder = binding.surfaceView.holder
        surfaceHolder.addCallback(this)

        // Check for camera permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }

        // Toggle between front and back camera
        binding.btnToggleCamera.setOnClickListener {
            switchCamera()
        }

        // Toggle flashlight
        binding.btnToggleFlash.setOnClickListener {
            toggleFlash()
        }

        // Capture photo
        binding.btnCapture.setOnClickListener {
            capturePhoto()
        }
    }

    private fun switchCamera() {
        isFrontCamera = !isFrontCamera
        releaseCamera()
        openCamera()
    }

    private fun toggleFlash() {
        if (isFlashAvailable()) {
            if (isFlashOn) {
                turnOffFlash()
            } else {
                turnOnFlash()
            }
        }
    }

    private fun isFlashAvailable(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    private fun turnOnFlash() {
        if (camera != null) {
            val parameters = camera!!.parameters
            parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            camera!!.parameters = parameters
            camera!!.startPreview()
            isFlashOn = true
        }
    }

    private fun turnOffFlash() {
        if (camera != null) {
            val parameters = camera!!.parameters
            parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
            camera!!.parameters = parameters
            camera!!.stopPreview()
            isFlashOn = false
        }
    }

    private fun capturePhoto() {
        if (camera != null) {
            camera?.takePicture(null, null, Camera.PictureCallback { data, camera ->
                // Create a file to save the image (you can customize the file path)
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val fileName = "IMG_$timeStamp.jpg"
                val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)

                try {
                    val fos = FileOutputStream(file)
                    fos.write(data)
                    fos.close()
                    // Notify the media scanner to add the captured image to the gallery
                    MediaScannerConnection.scanFile(
                        this,
                        arrayOf(file.toString()),
                        null,
                        null
                    )

                    // Restart the camera preview
                    camera?.startPreview()

                    // Provide feedback to the user (you can customize this)
                    Toast.makeText(
                        applicationContext,
                        "Image saved: ${file.absolutePath}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            })
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (surfaceHolder.surface == null) {
            return
        }

        try {
            camera?.stopPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val parameters = camera?.parameters
        val previewSize = getOptimalPreviewSize(width, height, parameters?.supportedPreviewSizes)
        parameters?.setPreviewSize(previewSize?.width ?: 640, previewSize?.height ?: 480)
        camera?.parameters = parameters

        // Start preview
        try {
            camera?.setPreviewDisplay(surfaceHolder)
            camera?.startPreview()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        openCamera()
    }

    private fun openCamera() {
        releaseCamera()
        try {
            camera = Camera.open(if (isFrontCamera) Camera.CameraInfo.CAMERA_FACING_FRONT else Camera.CameraInfo.CAMERA_FACING_BACK)
            camera?.setPreviewDisplay(surfaceHolder)
            camera?.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun releaseCamera() {
        camera?.apply {
            stopPreview()
            release()
        }
        camera = null
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
    private fun getOptimalPreviewSize(
        width: Int,
        height: Int,
        sizes: List<Camera.Size>?
    ): Camera.Size? {
        val targetRatio = width.toDouble() / height
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        sizes?.forEach { size ->
            val ratio = size.width.toDouble() / size.height
            val diff = Math.abs(ratio - targetRatio)
            if (diff < minDiff) {
                optimalSize = size
                minDiff = diff
            }
        }

        return optimalSize
    }
}



