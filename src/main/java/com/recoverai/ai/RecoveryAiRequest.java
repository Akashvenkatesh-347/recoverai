package com.recoverai.ai;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RecoveryAiRequest(

        @DecimalMin(value = "0.0", inclusive = false, message = "Payment amount must be greater than 0")
        double paymentAmount,

        @NotBlank(message = "Currency is required")
        String currency,

        @NotBlank(message = "Payment status is required")
        String paymentStatus,

        @NotBlank(message = "Failure reason is required")
        String failureReason,

        @Min(value = 0, message = "Retry count cannot be negative")
        int retryCount,

        @NotBlank(message = "Subscription status is required")
        String subscriptionStatus,

        @NotBlank(message = "Plan name is required")
        String planName
) {
}