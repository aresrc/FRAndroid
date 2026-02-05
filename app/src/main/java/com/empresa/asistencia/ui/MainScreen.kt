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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.empresa.asistencia.domain.FaceAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Observar estados del ViewModel usando collectAsState
    val employees by viewModel.employees.collectAsState()
    val detectedName by viewModel.detectedName.collectAsState()
    val distanceMetric by viewModel.distanceMetric.collectAsState()
    val showRegisterDialog by viewModel.showRegisterDialog.collectAsState()
    val showEmployeeList by viewModel.showEmployeeList.collectAsState()
    val newEmployeeName by viewModel.newEmployeeName.collectAsState()
    val lastEmbedding by viewModel.lastEmbedding.collectAsState()

    // Gestión de permisos de cámara
    var hasPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Se requiere permiso de cámara")
        }
        return
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Capa 1: Vista Previa de Cámara
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().apply {
                            setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(640, 480))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        // Configurar el analizador con lógica del ViewModel
                        imageAnalysis.setAnalyzer(
                            Executors.newSingleThreadExecutor(),
                            FaceAnalyzer(
                                recognizer = viewModel.recognizer,
                                getEmployeeList = { employees },
                                onFaceDetected = { /* Opcional: manejar rectángulos */ },
                                onPersonIdentified = { name, dist, embedding ->
                                    viewModel.onFaceIdentified(name, dist, embedding)
                                }
                            )
                        )

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

            // Capa 2: Información de Detección (Overlay)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), shape = MaterialTheme.shapes.medium)
                    .padding(12.dp)
            ) {
                Text("Persona: $detectedName", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text("Distancia: ${"%.2f".format(distanceMetric)}", color = Color.White)
            }

            // Capa 3: Botones de Acción
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.showRegisterDialog.value = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = lastEmbedding != null
                ) {
                    Text("Registrar Rostro Actual")
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.showEmployeeList.value = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ver Lista")
                    }
                    Button(
                        onClick = { viewModel.loadEmployees() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Actualizar")
                    }
                }
            }
        }
    }

    // --- Diálogos de la UI ---

    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showRegisterDialog.value = false },
            title = { Text("Nuevo Registro") },
            text = {
                Column {
                    Text("Ingrese el nombre para este rostro:")
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = newEmployeeName,
                        onValueChange = { viewModel.newEmployeeName.value = it },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.saveEmployee() }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showRegisterDialog.value = false }) { Text("Cancelar") }
            }
        )
    }

    if (showEmployeeList) {
        AlertDialog(
            onDismissRequest = { viewModel.showEmployeeList.value = false },
            title = { Text("Empleados Registrados") },
            text = {
                Box(modifier = Modifier.height(300.dp)) {
                    if (employees.isEmpty()) {
                        Text("No hay registros", modifier = Modifier.align(Alignment.Center))
                    } else {
                        LazyColumn {
                            items(employees) { emp ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(emp.name, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { viewModel.deleteEmployee(emp.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showEmployeeList.value = false }) { Text("Cerrar") }
            }
        )
    }
}