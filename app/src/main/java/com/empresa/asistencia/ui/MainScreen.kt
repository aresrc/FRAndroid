package com.empresa.asistencia.ui


import android.Manifest
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.empresa.asistencia.data.Employee
import com.empresa.asistencia.data.EmployeeRepository
import com.empresa.asistencia.domain.FaceAnalyzer
import com.empresa.asistencia.domain.FaceRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Estados
    var hasPermission by remember { mutableStateOf(false) }
    var detectedName by remember { mutableStateOf("Esperando...") }
    var distanceMetric by remember { mutableStateOf(0f) }

    // Repositorio e IA
    val repository = remember { EmployeeRepository(context) }
    val recognizer = remember { FaceRecognizer(context) }

    // Lista de empleados en memoria para búsqueda rápida
    var employees by remember { mutableStateOf(emptyList<Employee>()) }

    // Cargar empleados al iniciar
    LaunchedEffect(Unit) {
        employees = withContext(Dispatchers.IO) { repository.getAllEmployees() }
    }

    // Permisos
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasPermission) {
        Box(modifier = Modifier.fillMaxSize()) {

            // 1. Vista de Cámara
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        // Configurar Preview
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        // Configurar Análisis de Imagen (IA)
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(640, 480)) // Baja res para velocidad
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(),
                            FaceAnalyzer(
                                recognizer = recognizer,
                                employeeList = employees,
                                onFaceDetected = { /* Dibujar cuadro opcional */ },
                                onPersonIdentified = { name, dist ->
                                    detectedName = name
                                    distanceMetric = dist

                                    // Lógica de Registro (ejemplo simple)
                                    if (name != "Desconocido" && name != "Sin rostro") {
                                        // Aquí podrías agregar un delay para no registrar 100 veces por segundo
                                        // repository.logAttendance(...)
                                    }
                                }
                            )
                        )

                        // Usar cámara frontal
                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // 2. Interfaz de Usuario (Overlay)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (detectedName == "Desconocido") "Acceso Denegado" else "Hola, $detectedName",
                    color = if (detectedName == "Desconocido") Color.Red else Color.Green,
                    fontSize = 24.sp,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "Confianza: ${"%.2f".format(1.0f - distanceMetric)}", // Inverso de distancia para mostrar "confianza"
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Botón SIMULADO para registrar empleado (En app real, ocultar esto)
                Button(onClick = {
                    // Simular captura actual como nuevo empleado
                    // En producción: Navegar a pantalla de registro y tomar foto controlada
                    // Esto requiere lógica extra en el Analyzer para devolver el embedding actual
                }) {
                    Text("Modo Admin: Registrar")
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Se necesita permiso de cámara")
        }
    }
}