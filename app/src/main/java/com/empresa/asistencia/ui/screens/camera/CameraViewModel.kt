/*
 * Ruta: app/src/main/java/com/empresa/asistencia/ui/screens/camera/CameraViewModel.kt
 * Descripción: Maneja la lógica de detección, reconocimiento y registro de rostros.
 */
package com.empresa.asistencia.ui.screens.camera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.empresa.asistencia.data.EmployeeRepository
import com.empresa.asistencia.domain.FaceRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CameraUiState(
    val detectedName: String? = null,
    val isAccessGranted: Boolean? = null,
    val showRegisterDialog: Boolean = false,
    val isProcessing: Boolean = false
)

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val faceRecognizer: FaceRecognizer,
    private val repository: EmployeeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var lastEmbeddings: FloatArray? = null

    fun onFaceDetected(embeddings: FloatArray?) {
        if (_uiState.value.isProcessing || embeddings == null) return

        _uiState.value = _uiState.value.copy(isProcessing = true)

        viewModelScope.launch {
            val result = faceRecognizer.recognizeFace(embeddings)
            if (result != null) {
                _uiState.value = _uiState.value.copy(
                    detectedName = result.first,
                    isAccessGranted = true,
                    isProcessing = false
                )
            } else {
                lastEmbeddings = embeddings
                _uiState.value = _uiState.value.copy(
                    detectedName = "Desconocido",
                    isAccessGranted = false,
                    showRegisterDialog = true,
                    isProcessing = false
                )
            }
        }
    }

    fun registerEmployee(name: String) {
        val embeddings = lastEmbeddings ?: return
        viewModelScope.launch {
            repository.saveEmployee(name, embeddings)
            _uiState.value = _uiState.value.copy(
                showRegisterDialog = false,
                detectedName = name,
                isAccessGranted = true
            )
        }
    }

    fun dismissRegisterDialog() {
        _uiState.value = _uiState.value.copy(showRegisterDialog = false)
    }
}