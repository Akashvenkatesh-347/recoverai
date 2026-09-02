package com.recoverai.recovery;

import com.recoverai.payment.FailureReason;
import com.recoverai.payment.Payment;
import com.recoverai.subscription.SubscriptionStatus;
import org.springframework.stereotype.Service;

@Service
public class RecoveryPolicyService {

    private static final int MAX_RETRIES = 3;

    public RecommendedAction determineAllowedAction(
            Payment payment,
            RecommendedAction aiRecommendation) {

        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }

        if (aiRecommendation == null) {
            return RecommendedAction.NO_ACTION;
        }

        if (payment.getStatus() != com.recoverai.payment.PaymentStatus.FAILED) {
            return RecommendedAction.NO_ACTION;
        }

        if (payment.getSubscription() == null) {
            return RecommendedAction.ESCALATE;
        }

        if (payment.getSubscription().getStatus() != SubscriptionStatus.ACTIVE) {
            return RecommendedAction.USER_NOTIFICATION;
        }

        FailureReason failureReason = payment.getFailureReason();

        if (failureReason == null || !failureReason.isRetryable()) {
            return RecommendedAction.USER_NOTIFICATION;
        }

        if (payment.getRetryCount() >= MAX_RETRIES) {
            return RecommendedAction.ESCALATE;
        }

        if (aiRecommendation == RecommendedAction.RETRY_PAYMENT) {
            return RecommendedAction.RETRY_PAYMENT;
        }

        return aiRecommendation;
    }
}