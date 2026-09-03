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
    private final RecoveryAttemptRepository recoveryAttemptRepository;

    public RecoveryExecutionService(
            PaymentRepository paymentRepository,
            RecoveryAttemptRepository recoveryAttemptRepository) {

        this.paymentRepository = paymentRepository;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
    }

    @Transactional
    public String execute(Payment payment, RecommendedAction action) {

        if (payment == null) {
            throw new IllegalArgumentException("Payment cannot be null");
        }

        if (action == null) {
            throw new IllegalArgumentException(
                    "Recovery action cannot be null"
            );
        }

        return switch (action) {

            case RETRY_PAYMENT -> executeRetry(payment);

            case USER_NOTIFICATION ->
                    recordNonRetryAction(
                            payment,
                            action,
                            RecoveryAttemptStatus.SUCCESS,
                            "User notification required for payment "
                                    + payment.getId()
                    );

            case ESCALATE ->
                    recordNonRetryAction(
                            payment,
                            action,
                            RecoveryAttemptStatus.SUCCESS,
                            "Payment " + payment.getId()
                                    + " has been escalated for manual review."
                    );

            case NO_ACTION ->
                    recordNonRetryAction(
                            payment,
                            action,
                            RecoveryAttemptStatus.SKIPPED,
                            "No recovery action required for payment "
                                    + payment.getId()
                    );
        };
    }

    private String executeRetry(Payment payment) {

        if (payment.getStatus() != PaymentStatus.FAILED) {

            String result = "Payment " + payment.getId()
                    + " is not in FAILED state. Retry skipped.";

            saveAttempt(
                    payment,
                    RecommendedAction.RETRY_PAYMENT,
                    RecoveryAttemptStatus.SKIPPED,
                    payment.getRetryCount(),
                    result
            );

            return result;
        }

        int retryCountBefore = payment.getRetryCount();

        payment.setRetryCount(retryCountBefore + 1);
        payment.setAttemptedAt(LocalDateTime.now());

        paymentRepository.save(payment);

        String result = "Retry scheduled for payment "
                + payment.getId()
                + ". Retry count: "
                + payment.getRetryCount();

        saveAttempt(
                payment,
                RecommendedAction.RETRY_PAYMENT,
                RecoveryAttemptStatus.SUCCESS,
                retryCountBefore,
                result
        );

        return result;
    }

    private String recordNonRetryAction(
            Payment payment,
            RecommendedAction action,
            RecoveryAttemptStatus status,
            String result) {

        saveAttempt(
                payment,
                action,
                status,
                payment.getRetryCount(),
                result
        );

        return result;
    }

    private void saveAttempt(
            Payment payment,
            RecommendedAction action,
            RecoveryAttemptStatus status,
            int retryCountBefore,
            String result) {

        RecoveryAttempt attempt = new RecoveryAttempt();

        attempt.setPayment(payment);
        attempt.setAction(action);
        attempt.setStatus(status);
        attempt.setRetryCountBefore(retryCountBefore);
        attempt.setAttemptedAt(LocalDateTime.now());
        attempt.setResult(result);

        recoveryAttemptRepository.save(attempt);
    }
}
