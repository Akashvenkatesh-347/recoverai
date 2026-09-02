package com.recoverai.ai;

public record RecoveryAiRequest(
        double paymentAmount,
        String currency,
        String paymentStatus,
        String failureReason,
        int retryCount,
        String subscriptionStatus,
        String planName
) {
}