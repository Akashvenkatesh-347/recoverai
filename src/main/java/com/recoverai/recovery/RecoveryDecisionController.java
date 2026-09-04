package com.recoverai.recovery;

import com.recoverai.ai.RecoveryAiRequest;
import com.recoverai.ai.RecoveryAiResponse;
import com.recoverai.ai.RecoveryAiService;
import com.recoverai.payment.Payment;
import com.recoverai.payment.repository.PaymentRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recovery")
public class RecoveryDecisionController {

    private final PaymentRepository paymentRepository;
    private final RecoveryPolicyService recoveryPolicyService;
    private final RecoveryExecutionService recoveryExecutionService;
    private final RecoveryAttemptRepository recoveryAttemptRepository;
    private final RecoveryAiService recoveryAiService;

    public RecoveryDecisionController(
            PaymentRepository paymentRepository,
            RecoveryPolicyService recoveryPolicyService,
            RecoveryExecutionService recoveryExecutionService,
            RecoveryAttemptRepository recoveryAttemptRepository,
            RecoveryAiService recoveryAiService) {

        this.paymentRepository = paymentRepository;
        this.recoveryPolicyService = recoveryPolicyService;
        this.recoveryExecutionService = recoveryExecutionService;
        this.recoveryAttemptRepository = recoveryAttemptRepository;
        this.recoveryAiService = recoveryAiService;
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

        RecoveryAiRequest aiRequest =
                new RecoveryAiRequest(paymentId);

        RecoveryAiResponse aiResponse =
                recoveryAiService.analyze(aiRequest);

        RecommendedAction finalAction =
                recoveryPolicyService.determineAllowedAction(
                        payment,
                        aiResponse.recommendedAction()
                );

        RecoveryDecisionResponse response =
                new RecoveryDecisionResponse(
                        payment.getId(),
                        aiResponse.riskLevel(),
                        aiResponse.recommendedAction(),
                        finalAction,
                        aiResponse.reason(),
                        aiResponse.confidence()
                );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/payments/{paymentId}/execute")
    public ResponseEntity<String> executeRecovery(
            @PathVariable Long paymentId,
            @Valid @RequestBody RecoveryExecuteRequest request) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found: " + paymentId
                        )
                );

        RecommendedAction finalAction =
                recoveryPolicyService.determineAllowedAction(
                        payment,
                        request.action()
                );

        String result =
                recoveryExecutionService.execute(
                        payment,
                        finalAction
                );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/payments/{paymentId}/history")
    public ResponseEntity<List<RecoveryHistoryResponse>> getRecoveryHistory(
            @PathVariable Long paymentId) {

        if (!paymentRepository.existsById(paymentId)) {
            throw new IllegalArgumentException(
                    "Payment not found: " + paymentId
            );
        }

        List<RecoveryHistoryResponse> history =
                recoveryAttemptRepository
                        .findByPaymentIdOrderByAttemptedAtDesc(paymentId)
                        .stream()
                        .map(attempt -> new RecoveryHistoryResponse(
                                attempt.getAction(),
                                attempt.getRetryCountBefore(),
                                attempt.getAttemptedAt(),
                                attempt.getResult()
                        ))
                        .toList();

        return ResponseEntity.ok(history);
    }
}