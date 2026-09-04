package com.recoverai.ai;

import com.recoverai.payment.FailureReason;
import com.recoverai.payment.Payment;
import com.recoverai.payment.PaymentStatus;
import com.recoverai.recovery.RecommendedAction;
import com.recoverai.recovery.RiskLevel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock-ai")
public class MockRecoveryAiProvider implements RecoveryAiProvider {

    @Override
    public RecoveryAiResponse recommend(Payment payment) {

        if (payment.getStatus() != PaymentStatus.FAILED) {
            return new RecoveryAiResponse(
                    RiskLevel.LOW,
                    "Payment is not in a failed state.",
                    RecommendedAction.NO_ACTION,
                    0.99
            );
        }

        if (payment.getFailureReason() == FailureReason.INSUFFICIENT_FUNDS) {
            return new RecoveryAiResponse(
                    RiskLevel.LOW,
                    "The payment failed because of insufficient funds and may succeed on a later retry.",
                    RecommendedAction.RETRY_PAYMENT,
                    0.92
            );
        }

        if (payment.getFailureReason() != null
                && payment.getFailureReason().isRetryable()) {

            return new RecoveryAiResponse(
                    RiskLevel.MEDIUM,
                    "The payment failure appears retryable.",
                    RecommendedAction.RETRY_PAYMENT,
                    0.85
            );
        }

        return new RecoveryAiResponse(
                RiskLevel.HIGH,
                "The payment failure does not appear suitable for automatic retry.",
                RecommendedAction.USER_NOTIFICATION,
                0.90
        );
    }
}