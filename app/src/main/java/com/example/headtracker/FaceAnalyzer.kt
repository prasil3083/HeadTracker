package com.example.headtracker

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    private val onFacePosition: (Float, Float) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    // CHANGE THIS NUMBER (0-7) TO TEST DIFFERENT COORDINATE MAPPINGS
    // Try each number from 0 to 7 until the dot aligns with your face
    private val COORDINATE_MODE = 7

    @androidx.camera.core.ExperimentalGetImage
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        detector.process(image)
            .addOnSuccessListener { faces: List<Face> ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    val bbox = face.boundingBox

                    // Image dimensions
                    val imgW = mediaImage.width.toFloat()
                    val imgH = mediaImage.height.toFloat()

                    // Raw coordinates (center of face)
                    val rawX = bbox.centerX().toFloat()
                    val rawY = bbox.centerY().toFloat()

                    // Apply coordinate transformation based on mode
                    val (normX, normY) = when (COORDINATE_MODE) {
                        0 -> {
                            // Mode 0: Direct mapping
                            Pair(rawX / imgW, rawY / imgH)
                        }
                        1 -> {
                            // Mode 1: Mirror X
                            Pair(1f - (rawX / imgW), rawY / imgH)
                        }
                        2 -> {
                            // Mode 2: Mirror Y
                            Pair(rawX / imgW, 1f - (rawY / imgH))
                        }
                        3 -> {
                            // Mode 3: Mirror both
                            Pair(1f - (rawX / imgW), 1f - (rawY / imgH))
                        }
                        4 -> {
                            // Mode 4: Swap X↔Y
                            Pair(rawY / imgH, rawX / imgW)
                        }
                        5 -> {
                            // Mode 5: Swap X↔Y, Mirror X
                            Pair(1f - (rawY / imgH), rawX / imgW)
                        }
                        6 -> {
                            // Mode 6: Swap X↔Y, Mirror Y
                            Pair(rawY / imgH, 1f - (rawX / imgW))
                        }
                        7 -> {
                            // Mode 7: Swap X↔Y, Mirror both
                            Pair(1f - (rawY / imgH), 1f - (rawX / imgW))
                        }
                        else -> Pair(rawX / imgW, rawY / imgH)
                    }

                    // Clamp values
                    val finalX = normX.coerceIn(0f, 1f)
                    val finalY = normY.coerceIn(0f, 1f)

                    Log.d("FaceAnalyzer", "Mode=$COORDINATE_MODE | Rot=$rotation° | Raw=(${rawX.toInt()},${rawY.toInt()}) | Norm=(${"%.2f".format(finalX)},${"%.2f".format(finalY)})")

                    onFacePosition(finalX, finalY)
                } else {
                    onFacePosition(-1f, -1f)
                }
            }
            .addOnFailureListener { e ->
                Log.e("FaceAnalyzer", "Face detection failed", e)
                onFacePosition(-1f, -1f)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun stop() {
        detector.close()
    }
}