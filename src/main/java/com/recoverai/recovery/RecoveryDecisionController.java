package com.recoverai.recovery;

import com.recoverai.payment.Payment;
import com.recoverai.payment.repository.PaymentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryDecisionController {

    private final PaymentRepository paymentRepository;
    private final RecoveryPolicyService recoveryPolicyService;
    private final RecoveryExecutionService recoveryExecutionService;

    public RecoveryDecisionController(
            PaymentRepository paymentRepository,
            RecoveryPolicyService recoveryPolicyService,
            RecoveryExecutionService recoveryExecutionService) {

        this.paymentRepository = paymentRepository;
        this.recoveryPolicyService = recoveryPolicyService;
        this.recoveryExecutionService = recoveryExecutionService;
    }

    @PostMapping("/payments/{paymentId}/decision")
    public ResponseEntity<RecoveryDecisionResponse> determineRecoveryDecision(
            @PathVariable Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found: " + paymentId
                        )
                );

        RecommendedAction action =
                recoveryPolicyService.determineAllowedAction(
                        payment,
                        RecommendedAction.RETRY_PAYMENT
                );

        RiskLevel riskLevel = determineRiskLevel(payment, action);

        String reason = buildReason(payment, action);

        RecoveryDecisionResponse response =
                new RecoveryDecisionResponse(
                        payment.getId(),
                        riskLevel,
                        action,
                        reason
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/{paymentId}/execute")
    public ResponseEntity<String> executeRecovery(
            @PathVariable Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found: " + paymentId
                        )
                );

        RecommendedAction action =
                recoveryPolicyService.determineAllowedAction(
                        payment,
                        RecommendedAction.RETRY_PAYMENT
                );

        String result =
                recoveryExecutionService.execute(payment, action);

        return ResponseEntity.ok(result);
    }

    private RiskLevel determineRiskLevel(
            Payment payment,
            RecommendedAction action) {

        return switch (action) {
            case RETRY_PAYMENT -> RiskLevel.LOW;
            case USER_NOTIFICATION -> RiskLevel.MEDIUM;
            case ESCALATE -> RiskLevel.HIGH;
            case NO_ACTION -> RiskLevel.LOW;
        };
    }

    private String buildReason(
            Payment payment,
            RecommendedAction action) {

        return switch (action) {
            case RETRY_PAYMENT ->
                    "Payment failed for a retryable reason and is within the retry limit.";

            case USER_NOTIFICATION ->
                    "Payment cannot currently be retried automatically.";

            case ESCALATE ->
                    "Payment requires manual intervention.";

            case NO_ACTION ->
                    "No recovery action is currently required.";
        };
    }
}