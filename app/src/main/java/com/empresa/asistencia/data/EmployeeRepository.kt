package com.empresa.asistencia.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

// Modelo de datos simple
data class Employee(
    val id: String,
    val name: String,
    val embedding: List<Float> // Vector de características faciales (128 o 192 floats)
)

class EmployeeRepository(private val context: Context) {
    private val gson = Gson()
    private val fileName = "employees_db.json"

    // Guardar lista de empleados
    fun saveEmployee(employee: Employee) {
        val currentList = getAllEmployees().toMutableList()
        // Eliminamos si ya existe para actualizarlo
        currentList.removeIf { it.id == employee.id }
        currentList.add(employee)

        val jsonString = gson.toJson(currentList)
        val file = File(context.filesDir, fileName)
        file.writeText(jsonString)
    }

    // Obtener todos los empleados
    fun getAllEmployees(): List<Employee> {
        val file = File(context.filesDir, fileName)
        if (!file.exists()) return emptyList()

        val jsonString = file.readText()
        val type = object : TypeToken<List<Employee>>() {}.type
        return gson.fromJson(jsonString, type) ?: emptyList()
    }

    fun deleteEmployee(id: String) {
        val currentList = getAllEmployees().toMutableList()
        // Eliminamos al empleado que coincida con el ID
        currentList.removeIf { it.id == id }

        val jsonString = gson.toJson(currentList)
        val file = File(context.filesDir, fileName)
        file.writeText(jsonString)
    }
    // Simular registro de asistencia (Log)
    fun logAttendance(employee: Employee) {
        // Aquí podrías guardar en otro JSON con fecha/hora
        println("ASISTENCIA REGISTRADA: ${employee.name} a las ${java.time.LocalDateTime.now()}")
    }
}