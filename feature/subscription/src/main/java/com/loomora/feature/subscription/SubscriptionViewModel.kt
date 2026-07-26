package com.loomora.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loomora.core.datastore.EntitlementManager
import com.loomora.core.model.EntitlementStatus
import com.loomora.core.model.LicenseValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SubscriptionUiState(
    val status: EntitlementStatus = EntitlementStatus.FreeTrial(3),
    val activationInput: String = "",
    val isActivating: Boolean = false,
    val activationMessage: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val entitlementManager: EntitlementManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SubscriptionUiState(status = entitlementManager.getEntitlementStatus())
    )
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    fun onActivationInputChanged(input: String) {
        _uiState.value = _uiState.value.copy(activationInput = input, activationMessage = null)
    }

    fun activateLicenseKey() {
        val input = _uiState.value.activationInput
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActivating = true)
            val result = entitlementManager.activateLicenseKey(input)
            when (result) {
                is LicenseValidationResult.Valid -> {
                    _uiState.value = _uiState.value.copy(
                        isActivating = false,
                        isSuccess = true,
                        status = entitlementManager.getEntitlementStatus(),
                        activationMessage = "Loomora Pro activated successfully!"
                    )
                }
                is LicenseValidationResult.Invalid -> {
                    _uiState.value = _uiState.value.copy(
                        isActivating = false,
                        isSuccess = false,
                        activationMessage = result.reason
                    )
                }
            }
        }
    }
}
