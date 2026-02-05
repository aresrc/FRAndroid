/*
 * Ruta: app/src/main/java/com/empresa/asistencia/ui/screens/camera/CameraScreen.kt
 * Descripción: Interfaz de usuario para la vista de cámara y detección en tiempo real.
 */
package com.empresa.asistencia.ui.screens.camera

import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.empresa.asistencia.domain.FaceAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToEmployeeList: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var nameToRegister by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Vista de Cámara
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                // Aquí iría la lógica de inicialización de CameraX vinculada al analyzer
                // Por ahora mantenemos la estructura visual
            }
        )

        // Overlay de información
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            uiState.detectedName?.let { name ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isAccessGranted == true) Color(0xFF4CAF50) else Color(0xFFF44336)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (uiState.isAccessGranted == true) "Hola, $name" else "Acceso Denegado",
                        modifier = Modifier.padding(16.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }

        // Botón para ir a la lista
        FloatingActionButton(
            onClick = onNavigateToEmployeeList,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.List, contentDescription = "Lista de Empleados")
        }

        // Diálogo de Registro
        if (uiState.showRegisterDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissRegisterDialog() },
                title = { Text("Nuevo Rostro Detectado") },
                text = {
                    TextField(
                        value = nameToRegister,
                        onValueChange = { nameToRegister = it },
                        label = { Text("Nombre del Empleado") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.registerEmployee(nameToRegister)
                        nameToRegister = ""
                    }) {
                        Text("Registrar")
                    }
                }
            )
        }
    }
}