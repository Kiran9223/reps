package com.reps.app.feature.food

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reps.app.core.domain.repository.FoodRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class BarcodeScanUiState {
    object Scanning : BarcodeScanUiState()
    object Loading : BarcodeScanUiState()
    data class Found(val foodId: Long) : BarcodeScanUiState()
    object NotFound : BarcodeScanUiState()
}

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val foodRepository: FoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BarcodeScanUiState>(BarcodeScanUiState.Scanning)
    val uiState: StateFlow<BarcodeScanUiState> = _uiState.asStateFlow()

    private var isProcessing = false

    fun onBarcodeDetected(barcode: String) {
        if (isProcessing) return
        isProcessing = true
        _uiState.value = BarcodeScanUiState.Loading

        viewModelScope.launch {
            try {
                val food = foodRepository.getFoodByBarcode(barcode).first { it != null || true }
                if (food != null) {
                    _uiState.value = BarcodeScanUiState.Found(food.id)
                } else {
                    _uiState.value = BarcodeScanUiState.NotFound
                }
            } catch (e: Exception) {
                _uiState.value = BarcodeScanUiState.NotFound
            } finally {
                isProcessing = false
            }
        }
    }

    fun resetToScanning() {
        _uiState.value = BarcodeScanUiState.Scanning
        isProcessing = false
    }
}
