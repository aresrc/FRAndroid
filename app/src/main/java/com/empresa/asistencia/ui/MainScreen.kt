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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Estados
    var hasPermission by remember { mutableStateOf(false) }
    var detectedName by remember { mutableStateOf("Esperando...") }
    var distanceMetric by remember { mutableFloatStateOf(0f) }
    var lastEmbedding by remember { mutableStateOf<List<Float>?>(null) }
    var showRegisterDialog by remember { mutableStateOf(false) }
    var newEmployeeName by remember { mutableStateOf("") }
    var showEmployeeList by remember { mutableStateOf(false) }

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
                                getEmployeeList = { employees },
                                onFaceDetected = { /* Dibujar cuadro opcional */ },
                                onPersonIdentified = { name, dist, embedding ->
                                    detectedName = name
                                    distanceMetric = dist
                                    lastEmbedding = embedding
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

            if (showRegisterDialog) {
                AlertDialog(
                    onDismissRequest = { showRegisterDialog = false },
                    title = { Text("Registrar Empleado") },
                    text = {
                        Column {
                            Text("Se detectó un rostro. Ingresa el nombre:")
                            TextField(
                                value = newEmployeeName,
                                onValueChange = { newEmployeeName = it },
                                placeholder = { Text("Nombre completo") }
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            val embedding = lastEmbedding
                            if (newEmployeeName.isNotBlank() && embedding != null) {
                                scope.launch(Dispatchers.IO) {
                                    val newEmployee = Employee(
                                        id = System.currentTimeMillis().toString(),
                                        name = newEmployeeName,
                                        embedding = embedding
                                    )
                                    repository.saveEmployee(newEmployee) // Guarda en JSON

                                    // Refrescar la lista en memoria para que lo reconozca de inmediato
                                    employees = repository.getAllEmployees()

                                    withContext(Dispatchers.Main) {
                                        showRegisterDialog = false
                                        newEmployeeName = ""
                                    }
                                }
                            }
                        }) { Text("Guardar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRegisterDialog = false }) { Text("Cancelar") }
                    }
                )
            }

            if (showEmployeeList) {
                AlertDialog(
                    onDismissRequest = { showEmployeeList = false },
                    title = { Text("Empleados Registrados") },
                    text = {
                        // Definimos un alto para que no ocupe toda la pantalla si hay muchos
                        Box(modifier = Modifier
                            .height(400.dp)
                            .fillMaxWidth()) {
                            if (employees.isEmpty()) {
                                Text("No hay empleados registrados.", modifier = Modifier.align(Alignment.Center))
                            } else {
                                LazyColumn {
                                    items(employees) { employee ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(text = employee.name, style = MaterialTheme.typography.bodyLarge)
                                                Text(text = "ID: ${employee.id}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                            }

                                            IconButton(onClick = {
                                                scope.launch(Dispatchers.IO) {
                                                    repository.deleteEmployee(employee.id)
                                                    // Actualizamos la lista local para que Compose refresque la UI
                                                    employees = repository.getAllEmployees()
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Borrar",
                                                    tint = Color.Red
                                                )
                                            }
                                        }
                                        HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showEmployeeList = false }) {
                            Text("Cerrar")
                        }
                    }
                )
            }

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

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Botón de Registrar (el que ya tenías)
                    Button(onClick = { if (lastEmbedding != null) showRegisterDialog = true }) {
                        Text("Registrar")
                    }

                    // NUEVO: Botón para ver la lista
                    Button(
                        onClick = { showEmployeeList = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Ver Lista")
                    }
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Se necesita permiso de cámara")
        }
    }
}