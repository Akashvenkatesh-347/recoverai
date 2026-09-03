package com.recoverai.recovery;

public record RecoveryDecisionResponse(
        Long paymentId,
        RiskLevel riskLevel,
        RecommendedAction recommendedAction,
        String reason
) {
}