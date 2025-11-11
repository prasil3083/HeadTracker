package com.example.headtracker

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.Face

class FaceAnalyzer(
    private val onFacePosition: (Float, Float) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(options)

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { faces: List<Face> ->
                if (faces.isNotEmpty()) {
                    val face = faces[0]
                    // ML Kit returns coordinates in image space — they often match PreviewView coordinates for many phones.
                    // We'll pass raw pixel coords; later you may need to map/scale to preview size.
                    val x = face.boundingBox.centerX().toFloat()
                    val y = face.boundingBox.top.toFloat()
                    onFacePosition(x, y)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun stop() {
        detector.close()
    }
}
