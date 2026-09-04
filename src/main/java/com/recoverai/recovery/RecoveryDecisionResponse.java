package com.recoverai.recovery;

public record RecoveryDecisionResponse(
        Long paymentId,
        RiskLevel riskLevel,
        RecommendedAction aiRecommendedAction,
        RecommendedAction finalAction,
        String reason,
        double confidence
) {
}