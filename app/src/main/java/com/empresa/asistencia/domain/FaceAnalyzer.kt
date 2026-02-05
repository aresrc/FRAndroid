/*
 * Ruta: app/src/main/java/com/empresa/asistencia/domain/FaceAnalyzer.kt
 * Descripción: Analizador de CameraX que detecta rostros con ML Kit y extrae embeddings.
 */
package com.empresa.asistencia.domain

import android.graphics.*
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream

class FaceAnalyzer(
    private val faceRecognizer: FaceRecognizer,
    private val onFaceDetected: (FloatArray?) -> Unit
) : ImageAnalysis.Analyzer {

    // Configuración de ML Kit para alta precisión en detección de rostros
    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(options)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces[0] // Procesamos solo el rostro principal
                        val bitmap = imageProxy.toBitmap()
                        if (bitmap != null) {
                            // Recortamos el área del rostro detectado
                            val faceBitmap = cropFace(bitmap, face.boundingBox)
                            // Extraemos los embeddings (vectores numéricos) del rostro
                            val embeddings = faceRecognizer.getEmbedding(faceBitmap)
                            onFaceDetected(embeddings)
                        }
                    } else {
                        onFaceDetected(null)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun cropFace(bitmap: Bitmap, boundingBox: Rect): Bitmap {
        // Aseguramos que el recorte esté dentro de los límites del bitmap
        val left = boundingBox.left.coerceAtLeast(0)
        val top = boundingBox.top.coerceAtLeast(0)
        val width = boundingBox.width().coerceAtMost(bitmap.width - left)
        val height = boundingBox.height().coerceAtMost(bitmap.height - top)

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    // Extensión para convertir ImageProxy de CameraX a Bitmap
    private fun ImageProxy.toBitmap(): Bitmap? {
        val nv21 = yuv420ToNv21(this)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    private fun yuv420ToNv21(image: ImageProxy): ByteArray {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        return nv21
    }
}