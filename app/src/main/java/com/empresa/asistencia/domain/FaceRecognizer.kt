package com.empresa.asistencia.domain


import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import kotlin.math.sqrt

class FaceRecognizer(context: Context) {

    // NOTA: Debes colocar el archivo 'mobile_face_net.tflite' en src/main/assets/
    private val modelName = "mobile_face_net.tflite"
    private var interpreter: Interpreter? = null

    // MobileFaceNet usa input de 112x112
    private val inputImageSize = 112
    // Dimensión del vector de salida (usualmente 128 o 192 dependiendo del modelo)
    private val outputEmbeddingSize = 192

    init {
        try {
            val options = Interpreter.Options()
            options.setNumThreads(4) // Usar 4 hilos para rapidez
            interpreter = Interpreter(FileUtil.loadMappedFile(context, modelName), options)
            android.util.Log.d("IA_DEBUG", "Modelo cargado con éxito") // Añade esto
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("IA_DEBUG", "Error cargando el modelo: ${e.message}")
        }
    }

    fun getFaceEmbedding(bitmap: Bitmap): List<Float> {
        if (interpreter == null) {
            android.util.Log.e("IA_DEBUG", "El intérprete es NULL, no se puede procesar")
            return emptyList()
        }

        // 1. Pre-procesamiento de la imagen
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(inputImageSize, inputImageSize, ResizeOp.ResizeMethod.BILINEAR))
            // Normalización estándar para MobileFaceNet (depende del modelo exacto)
            // Usualmente (valor - 127.5) / 128.0
            .add(NormalizeOp(127.5f, 128.0f))
            .build()

        var tensorImage = TensorImage.fromBitmap(bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // 2. Buffer de salida
        // Shape: [1, 192] -> Batch size 1, vector size 192
        val outputBuffer = Array(1) { FloatArray(outputEmbeddingSize) }

        // 3. Ejecutar Inferencia
        interpreter?.run(tensorImage.buffer, outputBuffer)

        // 4. Retornar como lista
        return outputBuffer[0].toList()
    }

    // Calcular distancia Euclidiana (L2)
    // Si la distancia es < 0.8 (aprox), es la misma persona
    fun calculateDistance(embedding1: List<Float>, embedding2: List<Float>): Float {
        var sum = 0.0f
        for (i in embedding1.indices) {
            val diff = embedding1[i] - embedding2[i]
            sum += diff * diff
        }
        return sqrt(sum)
    }

    fun close() {
        interpreter?.close()
    }
}