/*
 * Ruta: app/src/main/java/com/empresa/asistencia/MainActivity.kt
 * Descripción: Actividad principal actualizada con Hilt y Navigation Compose.
 */
package com.empresa.asistencia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.empresa.asistencia.ui.screens.camera.CameraScreen
import com.empresa.asistencia.ui.screens.camera.CameraViewModel
import com.empresa.asistencia.ui.screens.employees.EmployeeListScreen
import com.empresa.asistencia.ui.screens.employees.EmployeeListViewModel
import com.empresa.asistencia.ui.theme.AsistenciaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsistenciaTheme {
                AsistenciaNavGraph()
            }
        }
    }
}

@Composable
fun AsistenciaNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "camera"
    ) {
        // Pantalla de Cámara (Detección)
        composable("camera") {
            val viewModel: CameraViewModel = hiltViewModel()
            CameraScreen(
                viewModel = viewModel,
                onNavigateToEmployeeList = {
                    navController.navigate("employee_list")
                }
            )
        }

        // Pantalla de Lista de Empleados
        composable("employee_list") {
            val viewModel: EmployeeListViewModel = hiltViewModel()
            EmployeeListScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}