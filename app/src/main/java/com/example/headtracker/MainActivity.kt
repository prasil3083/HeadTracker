package com.example.headtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val CAMERA_PERMISSION = arrayOf(android.Manifest.permission.CAMERA)
    private val CAMERA_REQUEST_CODE = 111

    private lateinit var cameraExecutor: ExecutorService
    private var faceAnalyzer: FaceAnalyzer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(CAMERA_PERMISSION, CAMERA_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview setup
                val previewView = findViewById<PreviewView>(R.id.previewView)
                previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE

                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Overlay setup
                val overlay = findViewById<FaceOverlayView>(R.id.faceOverlay)

                // Face analyzer setup
                faceAnalyzer = FaceAnalyzer { x, y ->
                    runOnUiThread {
                        overlay.setNormalizedPosition(x, y)
                    }
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor, faceAnalyzer!!)
                    }

                // Camera selector - BACK CAMERA
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all before rebinding
                cameraProvider.unbindAll()

                // Bind to lifecycle
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )

                Log.d("MainActivity", "Camera started successfully")
                Toast.makeText(this, "Camera started. Check logs for coordinate mode.", Toast.LENGTH_LONG).show()

            } catch (e: Exception) {
                Log.e("MainActivity", "Camera initialization failed", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() =
        CAMERA_PERMISSION.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onDestroy() {
        super.onDestroy()
        faceAnalyzer?.stop()
        cameraExecutor.shutdown()
    }
}