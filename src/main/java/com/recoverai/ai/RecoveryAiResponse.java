package com.recoverai.ai;

import com.recoverai.recovery.RecommendedAction;
import com.recoverai.recovery.RiskLevel;

public record RecoveryAiResponse(
        RiskLevel riskLevel,
        String reason,
        RecommendedAction recommendedAction,
        double confidence
) {
}