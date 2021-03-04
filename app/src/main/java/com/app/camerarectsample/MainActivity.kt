package com.app.camerarectsample

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.app.camerarectsample.databinding.ActivityMainBinding
import com.app.camerarectsample.databinding.LayoutCameraBinding
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.layout_camera.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var imageFile: File? = null
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraBinding: LayoutCameraBinding
    private var camera: Camera? = null
    private var scaleGestureDetector: ScaleGestureDetector? = null
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraBinding = binding.cameraView

        binding.btnOk.setOnClickListener {
            binding.cameraView.root.visibility = View.VISIBLE
            binding.image.visibility = View.GONE
//            binding.borderView.visibility = View.VISIBLE
            binding.layout.visibility = View.GONE
        }
        binding.btnCancel.setOnClickListener { finish() }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
            //bindCameraUseCases()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
        cameraBinding = binding.cameraView
        cameraBinding.cameraCaptureButton.setOnClickListener { takePhoto() }
        cameraBinding.btnFlash.setOnClickListener {
            when (flashMode) {
                ImageCapture.FLASH_MODE_OFF -> {
                    flashMode = ImageCapture.FLASH_MODE_ON
                    cameraBinding.btnFlash.setImageResource(R.drawable.ic_flash)
                }
                ImageCapture.FLASH_MODE_ON -> {
                    flashMode = ImageCapture.FLASH_MODE_AUTO
                    cameraBinding.btnFlash.setImageResource(R.drawable.ic_flash_auto)
                }
                ImageCapture.FLASH_MODE_AUTO -> {
                    flashMode = ImageCapture.FLASH_MODE_OFF
                    cameraBinding.btnFlash.setImageResource(R.drawable.ic_flash_off)
                }
            }
            startCamera()
            //Toast.makeText(applicationContext, "flash: " + flashMode, Toast.LENGTH_SHORT).show()
        }
        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        val listener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scale = camera?.cameraInfo?.zoomState?.value?.zoomRatio!! * detector.scaleFactor
                camera!!.cameraControl.setZoomRatio(scale)
                return true
            }
        }
        scaleGestureDetector = ScaleGestureDetector(applicationContext, listener)

        binding.cameraView.viewFinder.setOnTouchListener { _, event ->
            Log.e(TAG, "touching othr")
            scaleGestureDetector?.onTouchEvent(event)
            return@setOnTouchListener true
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT, Locale.US
            ).format(System.currentTimeMillis()) + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    onImageCaptured(savedUri)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)

                }
            })
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull().let {
            File(
                it,
                resources.getString(R.string.app_name)
            ).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    @SuppressLint("UnsafeExperimentalUsageError", "ClickableViewAccessibility")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(cameraBinding.viewFinder.createSurfaceProvider())
                }

            imageCapture = ImageCapture.Builder()
                .setFlashMode(flashMode)
                .build()

//            val imageAnalyzer = ImageAnalysis.Builder()
//                .build()
//                .also {
//                    it.setAnalyzer(cameraExecutor,
//                        QrCodeAnalyzer { qrResult ->
//                            cameraBinding.viewFinder.post {
//                                Log.d("QRCodeAnalyzer", "Barcode scanned: ${qrResult.text}")
//                                Toast.makeText(
//                                    this,
//                                    "Tag Added: ${qrResult.text}",
//                                    Toast.LENGTH_SHORT
//                                ).show()
//
//                                onQrDetected(qrResult.text)
//
//                            }
//                            Thread.sleep(5000)
//                        })
//                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val viewPort = ViewPort.Builder(Rational(400, 180), Surface.ROTATION_0).build()
            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
//                .addUseCase(imageAnalyzer)
                .addUseCase(imageCapture!!)
                .setViewPort(viewPort)
                .build()

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, useCaseGroup
                )
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, /*imageAnalyzer*/
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                // finish()
            }
            return
        }
    }

    companion object {
        private const val TAG = "AddTaskDialog"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    }

//    private fun onQrDetected(text: String) {
//        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
//    }

    private fun onImageCaptured(uri: Uri) {
        val file = File(uri.path!!)
        imageFile = file

        Glide.with(binding.image).load(file).into(binding.image)
        showImage()
    }

    private fun showImage() {
//        binding.borderView.visibility = View.GONE
        binding.cameraView.root.visibility = View.GONE
        binding.image.visibility = View.VISIBLE
        binding.layout.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}