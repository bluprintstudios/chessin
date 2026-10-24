package com.pro.chess_review.domain.model

data class EngineEvidenceItem(
    val uci: String,
    val san: String,
    val scoreCp: Int?,
    val mateIn: Int?,
    val depth: Int,
    val pvSan: List<String> = listOf(san),
    val pvUci: List<String> = listOf(uci),
) : java.io.Serializable

data class CoachRequest(
    val version: String = "2",
    val fen: String,
    val question: String,
    val playedMove: String,
    val engineEvidence: List<EngineEvidenceItem>,
) : java.io.Serializable

data class CoachResponse(
    val version: String,
    val explanation: String,
    val groundedCandidates: List<String>,
    val disclaimer: String,
)

data class AiConsentState(
    val hasConsented: Boolean,
    val usageCount: Int,
    val fairUseLimit: Int = 50,
) {
    val isLimitReached: Boolean get() = usageCount >= fairUseLimit
}
