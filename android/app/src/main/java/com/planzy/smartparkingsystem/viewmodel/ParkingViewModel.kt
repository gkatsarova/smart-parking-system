package com.planzy.smartparkingsystem.viewmodel


import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planzy.smartparkingsystem.data.CaptureResult
import com.planzy.smartparkingsystem.data.FirebaseResult
import com.planzy.smartparkingsystem.data.ParkingRepository
import com.planzy.smartparkingsystem.data.ParkingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ParkingUiState(
    val parking: ParkingState = ParkingState(),
    val isLoading: Boolean = true,
    val captureStatus: CaptureStatus = CaptureStatus.Idle,
    val distanceMeters: Float = Float.MAX_VALUE,
    val inRadius: Boolean = false,
    val errorMessage: String? = null,
)

sealed class CaptureStatus {
    object Idle : CaptureStatus()
    object Capturing : CaptureStatus()
    object Sent : CaptureStatus()
    data class Failed(val reason: String) : CaptureStatus()
}

class ParkingViewModel(
    private val repository: ParkingRepository = ParkingRepository(),
) : ViewModel() {

    companion object {
        const val PARKING_LAT  = 42.6977
        const val PARKING_LON = 23.3219
        const val TRIGGER_RADIUS_M = 100f
        const val ESP32_IP = "192.168.1.100"
        private const val TAG = "ParkingVM"
    }

    private val _uiState = MutableStateFlow(ParkingUiState())
    val uiState: StateFlow<ParkingUiState> = _uiState.asStateFlow()

    private var alreadyTriggered = false

    init {
        observeFirebase()
    }

    private fun observeFirebase() {
        viewModelScope.launch {
            repository.observeParkingState().collect { result ->
                when (result) {
                    is FirebaseResult.Data -> {
                        _uiState.update {
                            it.copy(
                                parking = result.state,
                                isLoading = false,
                                errorMessage = null,
                            )
                        }
                    }
                    is FirebaseResult.Failure -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message,
                            )
                        }
                    }
                }
            }
        }
    }


    fun onLocationUpdate(location: Location) {
        val results = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            PARKING_LAT, PARKING_LON,
            results,
        )
        val distance = results[0]
        val inRadius = distance <= TRIGGER_RADIUS_M

        _uiState.update { it.copy(distanceMeters = distance, inRadius = inRadius) }

        if (inRadius && !alreadyTriggered) {
            Log.i(TAG, "In radius range (${distance}m) - start camera")
            alreadyTriggered = true
            triggerCapture()
        } else if (!inRadius && alreadyTriggered) {
            Log.d(TAG, "Out of radius")
            alreadyTriggered = false
        }
    }


    fun triggerCapture() {
        if (_uiState.value.captureStatus is CaptureStatus.Capturing) return

        viewModelScope.launch {
            _uiState.update { it.copy(captureStatus = CaptureStatus.Capturing) }

            when (val result = repository.triggerCapture(ESP32_IP)) {
                is CaptureResult.Success -> {
                    _uiState.update { it.copy(captureStatus = CaptureStatus.Sent) }
                    kotlinx.coroutines.delay(4_000)
                    _uiState.update { it.copy(captureStatus = CaptureStatus.Idle) }
                }
                is CaptureResult.Error -> {
                    _uiState.update { it.copy(captureStatus = CaptureStatus.Failed(result.message)) }
                    kotlinx.coroutines.delay(30_000)
                    alreadyTriggered = false
                    _uiState.update { it.copy(captureStatus = CaptureStatus.Idle) }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}