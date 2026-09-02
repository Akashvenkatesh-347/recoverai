package com.recoverai.recovery;

import com.recoverai.payment.FailureReason;
import com.recoverai.payment.Payment;
import com.recoverai.payment.PaymentStatus;
import com.recoverai.subscription.Subscription;
import com.recoverai.subscription.SubscriptionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecoveryPolicyServiceTest {

    private final RecoveryPolicyService policyService =
            new RecoveryPolicyService();

    @Test
    void shouldAllowRetryForRetryableFailedPayment() {

        Payment payment = createPayment(
                PaymentStatus.FAILED,
                FailureReason.INSUFFICIENT_FUNDS,
                SubscriptionStatus.ACTIVE,
                0
        );

        RecommendedAction result =
                policyService.determineAllowedAction(
                        payment,
                        RecommendedAction.RETRY_PAYMENT
                );

        assertEquals(
                RecommendedAction.RETRY_PAYMENT,
                result
        );
    }

    @Test
    void shouldEscalateWhenMaximumRetriesReached() {

        Payment payment = createPayment(
                PaymentStatus.FAILED,
                FailureReason.INSUFFICIENT_FUNDS,
                SubscriptionStatus.ACTIVE,
                3
        );

        RecommendedAction result =
                policyService.determineAllowedAction(
                        payment,
                        RecommendedAction.RETRY_PAYMENT
                );

        assertEquals(
                RecommendedAction.ESCALATE,
                result
        );
    }

    @Test
    void shouldNotifyUserForNonRetryableFailure() {

        Payment payment = createPayment(
                PaymentStatus.FAILED,
                FailureReason.CARD_EXPIRED,
                SubscriptionStatus.ACTIVE,
                0
        );

        RecommendedAction result =
                policyService.determineAllowedAction(
                        payment,
                        RecommendedAction.RETRY_PAYMENT
                );

        assertEquals(
                RecommendedAction.USER_NOTIFICATION,
                result
        );
    }

    @Test
    void shouldNotifyUserForInactiveSubscription() {

        Payment payment = createPayment(
                PaymentStatus.FAILED,
                FailureReason.INSUFFICIENT_FUNDS,
                SubscriptionStatus.CANCELLED,
                0
        );

        RecommendedAction result =
                policyService.determineAllowedAction(
                        payment,
                        RecommendedAction.RETRY_PAYMENT
                );

        assertEquals(
                RecommendedAction.USER_NOTIFICATION,
                result
        );
    }

    @Test
    void shouldDoNothingForSuccessfulPayment() {

        Payment payment = createPayment(
                PaymentStatus.SUCCESS,
                null,
                SubscriptionStatus.ACTIVE,
                0
        );

        RecommendedAction result =
                policyService.determineAllowedAction(
                        payment,
                        RecommendedAction.RETRY_PAYMENT
                );

        assertEquals(
                RecommendedAction.NO_ACTION,
                result
        );
    }

    @Test
    void shouldRespectNonRetryRecommendation() {

        Payment payment = createPayment(
                PaymentStatus.FAILED,
                FailureReason.INSUFFICIENT_FUNDS,
                SubscriptionStatus.ACTIVE,
                0
        );

        RecommendedAction result =
                policyService.determineAllowedAction(
                        payment,
                        RecommendedAction.USER_NOTIFICATION
                );

        assertEquals(
                RecommendedAction.USER_NOTIFICATION,
                result
        );
    }

    private Payment createPayment(
            PaymentStatus paymentStatus,
            FailureReason failureReason,
            SubscriptionStatus subscriptionStatus,
            int retryCount) {

        Subscription subscription = new Subscription();
        subscription.setStatus(subscriptionStatus);

        Payment payment = new Payment();
        payment.setSubscription(subscription);
        payment.setAmount(new BigDecimal("649.00"));
        payment.setCurrency("INR");
        payment.setStatus(paymentStatus);
        payment.setFailureReason(failureReason);
        payment.setRetryCount(retryCount);

        return payment;
    }
}