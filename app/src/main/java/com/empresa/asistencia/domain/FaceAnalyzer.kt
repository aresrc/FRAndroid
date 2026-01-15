package com.empresa.asistencia.domain


import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.empresa.asistencia.data.Employee
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class FaceAnalyzer(
    private val recognizer: FaceRecognizer,
    private val employeeList: List<Employee>,
    private val onPersonIdentified: (String, Float) -> Unit, // Nombre, Distancia
    private val onFaceDetected: (Rect) -> Unit // Para dibujar el cuadro
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    )

    // Threshold: Ajustar según pruebas. Menor es más estricto.
    // 0.7 - 0.9 es común para MobileFaceNet
    private val similarityThreshold = 0.8f

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            detector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        val face = faces.first() // Asumimos una persona a la vez
                        onFaceDetected(face.boundingBox)

                        // Aquí es donde convertimos el frame a Bitmap y recortamos la cara
                        // NOTA: Esta conversión es costosa. En producción, optimizar con YUV to RGB converters
                        val bitmap = imageProxy.toBitmap()
                        val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees.toFloat())

                        // Recortar la cara (asegurando coordenadas válidas)
                        val cropRect = getValidRect(face.boundingBox, rotatedBitmap.width, rotatedBitmap.height)
                        val faceBitmap = Bitmap.createBitmap(
                            rotatedBitmap,
                            cropRect.left, cropRect.top,
                            cropRect.width(), cropRect.height()
                        )

                        // Obtener vector
                        val currentEmbedding = recognizer.getFaceEmbedding(faceBitmap)

                        // Buscar en la base de datos (1 vs N)
                        var identifiedName = "Desconocido"
                        var minDistance = Float.MAX_VALUE

                        for (employee in employeeList) {
                            val distance = recognizer.calculateDistance(currentEmbedding, employee.embedding)
                            if (distance < minDistance) {
                                minDistance = distance
                            }
                            if (distance < similarityThreshold) {
                                identifiedName = employee.name
                                // Encontramos al empleado, rompemos el bucle (o buscamos el mejor match)
                                break
                            }
                        }

                        onPersonIdentified(identifiedName, minDistance)
                    } else {
                        onPersonIdentified("Sin rostro", 0f)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        // Espejo si es cámara frontal
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun getValidRect(rect: Rect, width: Int, height: Int): Rect {
        val left = rect.left.coerceAtLeast(0)
        val top = rect.top.coerceAtLeast(0)
        val right = rect.right.coerceAtMost(width)
        val bottom = rect.bottom.coerceAtMost(height)
        return Rect(left, top, right, bottom)
    }
}