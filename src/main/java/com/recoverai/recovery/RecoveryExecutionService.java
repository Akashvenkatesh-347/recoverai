package com.recoverai.recovery;

import com.recoverai.payment.Payment;
import com.recoverai.payment.PaymentStatus;
import com.recoverai.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RecoveryExecutionService {

    private final PaymentRepository paymentRepository;

    public RecoveryExecutionService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public String execute(Payment payment, RecommendedAction action) {

        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }

        if (action == null) {
            throw new IllegalArgumentException("Recovery action cannot be null");
        }

        return switch (action) {

            case RETRY_PAYMENT -> executeRetry(payment);

            case USER_NOTIFICATION ->
                    "User notification required for payment " + payment.getId();

            case ESCALATE ->
                    "Payment " + payment.getId()
                            + " has been escalated for manual review.";

            case NO_ACTION ->
                    "No recovery action required for payment "
                            + payment.getId();
        };
    }

    private String executeRetry(Payment payment) {

        if (payment.getStatus() != PaymentStatus.FAILED) {
            return "Payment " + payment.getId()
                    + " is not in FAILED state. Retry skipped.";
        }

        payment.setRetryCount(payment.getRetryCount() + 1);
        payment.setAttemptedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        return "Retry scheduled for payment "
                + payment.getId()
                + ". Retry count: "
                + payment.getRetryCount();
    }
}