package com.recoverai.recovery;

import java.time.LocalDateTime;

public record RecoveryHistoryResponse(
        RecommendedAction action,
        int retryCountBefore,
        LocalDateTime attemptedAt,
        String result
) {
}