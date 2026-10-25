package com.pro.chess_review.feature.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pro.chess_review.domain.billing.BillingManager
import com.pro.chess_review.domain.billing.EntitlementState
import com.pro.chess_review.domain.model.AiConsentState
import com.pro.chess_review.domain.model.AppFailure
import com.pro.chess_review.domain.model.AppFailureException
import com.pro.chess_review.domain.model.CoachRequest
import com.pro.chess_review.domain.model.CoachResponse
import com.pro.chess_review.domain.usecase.GetCoachExplanationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CoachUiState(
    val consent: AiConsentState? = null,
    val response: CoachResponse? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class CoachViewModel(
    private val requestContext: CoachRequest,
    private val getCoachExplanation: GetCoachExplanationUseCase,
    private val billingManager: BillingManager,
    val serviceConfigured: Boolean,
    val isLocalDebugPreview: Boolean,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()
    val entitlementState: StateFlow<EntitlementState> = billingManager.entitlementState

    init {
        billingManager.startConnection()
        refreshConsent()
    }

    fun setConsent(granted: Boolean) {
        viewModelScope.launch {
            getCoachExplanation.setConsent(granted)
            refreshConsent()
        }
    }

    fun ask(question: String) {
        val normalizedQuestion = question.trim()
        val current = _uiState.value
        when {
            !serviceConfigured -> updateError("The AI service is not configured in this preview build.")
            current.consent?.hasConsented != true -> updateError("Review and accept the AI data notice before asking a question.")
            current.consent?.isLimitReached == true -> updateError("The preview AI request allowance has been reached.")
            requestContext.engineEvidence.isEmpty() -> updateError("There are no completed engine lines for this position yet.")
            normalizedQuestion.isEmpty() -> updateError("Enter a question about this position.")
            else -> {
                val purchaseToken = (entitlementState.value as? EntitlementState.Purchased)?.token
                if (purchaseToken.isNullOrBlank() && !isLocalDebugPreview) {
                    updateError("A verified Chessin' purchase is required for AI Coach.")
                    return
                }

                viewModelScope.launch {
                    _uiState.value = current.copy(isLoading = true, errorMessage = null)
                    val result = getCoachExplanation(
                        request = requestContext.copy(question = normalizedQuestion),
                        entitlementToken = purchaseToken,
                    )
                    val response = result.getOrNull()
                    if (response != null) {
                        _uiState.value = _uiState.value.copy(
                            response = response,
                            isLoading = false,
                            errorMessage = null,
                        )
                        refreshConsent()
                    } else {
                        _uiState.value = current.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull().toCoachMessage(),
                        )
                    }
                }
            }
        }
    }

    private fun refreshConsent() {
        viewModelScope.launch {
            val consent = getCoachExplanation.getConsentState()
            _uiState.value = _uiState.value.copy(consent = consent)
        }
    }

    private fun updateError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    override fun onCleared() {
        billingManager.destroy()
    }
}

private fun Throwable?.toCoachMessage(): String = when ((this as? AppFailureException)?.failure) {
    AppFailure.NetworkUnavailable -> "Chessin' couldn't reach the AI service. Your board and local review are still available."
    AppFailure.RateLimited -> "The AI request allowance is used up for now."
    AppFailure.PurchaseNotVerified -> "Play couldn't verify this purchase for AI access. Restore purchases or try again later."
    AppFailure.AiUnavailable -> "The AI service is temporarily unavailable. Try again later."
    else -> "Chessin' couldn't prepare an explanation. Please try again."
}

class CoachViewModelFactory(
    private val requestContext: CoachRequest,
    private val getCoachExplanation: GetCoachExplanationUseCase,
    private val billingManager: BillingManager,
    private val serviceConfigured: Boolean,
    private val isLocalDebugPreview: Boolean,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CoachViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return CoachViewModel(
            requestContext = requestContext,
            getCoachExplanation = getCoachExplanation,
            billingManager = billingManager,
            serviceConfigured = serviceConfigured,
            isLocalDebugPreview = isLocalDebugPreview,
        ) as T
    }
}
