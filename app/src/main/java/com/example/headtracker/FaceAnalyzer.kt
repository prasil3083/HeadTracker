package com.example.headtracker

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    private val listener: (x: Float, y: Float, imageWidth: Int, imageHeight: Int, rotation: Int) -> Unit
) : ImageAnalysis.Analyzer {

    companion object {
        private const val TAG = "FaceAnalyzer"
    }

    // Configure ML Kit face detector for fast performance
    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.15f) // Minimum face size relative to image
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        // Create InputImage from camera frame
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        // Process the image for face detection
        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    // Get the first detected face
                    val face = faces[0]
                    val boundingBox = face.boundingBox

                    // Calculate center of the face
                    val centerX = boundingBox.centerX().toFloat()
                    val centerY = boundingBox.centerY().toFloat()

                    // Pass coordinates along with image dimensions and rotation
                    listener(
                        centerX,
                        centerY,
                        inputImage.width,
                        inputImage.height,
                        imageProxy.imageInfo.rotationDegrees
                    )

                    Log.d(TAG, "Face detected at ($centerX, $centerY) - Size: ${inputImage.width}x${inputImage.height}, Rotation: ${imageProxy.imageInfo.rotationDegrees}")
                } else {
                    // No face detected - hide the dot
                    listener(-1f, -1f, inputImage.width, inputImage.height, imageProxy.imageInfo.rotationDegrees)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
            }
            .addOnCompleteListener {
                // Always close the image proxy to prevent blocking the pipeline
                imageProxy.close()
            }
    }
}