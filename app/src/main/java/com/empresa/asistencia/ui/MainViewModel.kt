package com.empresa.asistencia.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.empresa.asistencia.data.Employee
import com.empresa.asistencia.data.EmployeeRepository
import com.empresa.asistencia.domain.FaceRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EmployeeRepository(application)
    val recognizer = FaceRecognizer(application)

    // Estados de la UI
    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _detectedName = MutableStateFlow("Esperando...")
    val detectedName: StateFlow<String> = _detectedName.asStateFlow()

    private val _distanceMetric = MutableStateFlow(0f)
    val distanceMetric: StateFlow<Float> = _distanceMetric.asStateFlow()

    private val _lastEmbedding = MutableStateFlow<List<Float>?>(null)
    val lastEmbedding: StateFlow<List<Float>?> = _lastEmbedding.asStateFlow()

    // Estados de Diálogos
    var showRegisterDialog = MutableStateFlow(false)
    var showEmployeeList = MutableStateFlow(false)
    var newEmployeeName = MutableStateFlow("")

    init {
        loadEmployees()
    }

    fun loadEmployees() {
        viewModelScope.launch(Dispatchers.IO) {
            _employees.value = repository.getAllEmployees()
        }
    }

    fun onFaceIdentified(name: String, distance: Float, embedding: List<Float>?) {
        _detectedName.value = name
        _distanceMetric.value = distance
        _lastEmbedding.value = embedding
    }

    fun saveEmployee() {
        val name = newEmployeeName.value
        val embedding = _lastEmbedding.value
        if (name.isNotBlank() && embedding != null) {
            viewModelScope.launch(Dispatchers.IO) {
                val newEmployee = Employee(
                    id = System.currentTimeMillis().toString(),
                    name = name,
                    embedding = embedding
                )
                repository.saveEmployee(newEmployee)
                loadEmployees() // Refrescar lista
                newEmployeeName.value = ""
                showRegisterDialog.value = false
            }
        }
    }

    fun deleteEmployee(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteEmployee(id)
            loadEmployees()
        }
    }

    override fun onCleared() {
        super.onCleared()
        recognizer.close()
    }
}