/*
 * Ruta: app/src/main/java/com/empresa/asistencia/ui/screens/employees/EmployeeListViewModel.kt
 * Descripción: Maneja el estado de la lista de empleados y su eliminación.
 */
package com.empresa.asistencia.ui.screens.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empresa.asistencia.data.EmployeeRepository
import com.empresa.asistencia.data.Employee
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EmployeeListViewModel @Inject constructor(
    private val repository: EmployeeRepository
) : ViewModel() {

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    init {
        loadEmployees()
    }

    private fun loadEmployees() {
        viewModelScope.launch {
            _employees.value = repository.getAllEmployees()
        }
    }

    fun deleteEmployee(name: String) {
        viewModelScope.launch {
            repository.deleteEmployee(name)
            loadEmployees() // Recargar lista
        }
    }
}