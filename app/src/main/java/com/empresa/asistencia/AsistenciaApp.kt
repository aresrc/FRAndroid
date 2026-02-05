/*
 * Ruta: app/src/main/java/com/empresa/asistencia/AsistenciaApp.kt
 * Descripción: Clase Application necesaria para que Hilt genere el grafo de dependencias.
 */
package com.empresa.asistencia

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AsistenciaApp : Application()