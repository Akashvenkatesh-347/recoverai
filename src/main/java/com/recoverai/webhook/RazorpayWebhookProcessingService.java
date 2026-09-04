package com.recoverai.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recoverai.ai.RecoveryAiRequest;
import com.recoverai.ai.RecoveryAiResponse;
import com.recoverai.ai.RecoveryAiService;
import com.recoverai.payment.Payment;
import com.recoverai.payment.repository.PaymentRepository;
import com.recoverai.recovery.RecoveryExecutionService;
import com.recoverai.recovery.RecoveryPolicyService;
import com.recoverai.recovery.RecommendedAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RazorpayWebhookProcessingService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final PaymentRepository paymentRepository;
    private final RecoveryAiService recoveryAiService;
    private final RecoveryPolicyService recoveryPolicyService;
    private final RecoveryExecutionService recoveryExecutionService;

    public RazorpayWebhookProcessingService(
            PaymentRepository paymentRepository,
            RecoveryAiService recoveryAiService,
            RecoveryPolicyService recoveryPolicyService,
            RecoveryExecutionService recoveryExecutionService) {

        this.paymentRepository = paymentRepository;
        this.recoveryAiService = recoveryAiService;
        this.recoveryPolicyService = recoveryPolicyService;
        this.recoveryExecutionService = recoveryExecutionService;
    }

    @Transactional
    public String process(String payload) {

        try {
            JsonNode root = objectMapper.readTree(payload);

            String event = root.path("event").asText();

            if (!"payment.failed".equals(event)) {
                return "Event ignored: " + event;
            }

            String razorpayPaymentId = root
                    .path("payload")
                    .path("payment")
                    .path("entity")
                    .path("id")
                    .asText();

            if (razorpayPaymentId == null
                    || razorpayPaymentId.isBlank()) {

                return "Payment ID missing from webhook";
            }

            Payment payment = paymentRepository
                    .findAll()
                    .stream()
                    .filter(existingPayment ->
                            razorpayPaymentId.equals(
                                    existingPayment.getRazorpayPaymentId()
                            )
                    )
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Payment not found for Razorpay payment ID: "
                                            + razorpayPaymentId
                            )
                    );

            RecoveryAiResponse aiResponse =
                    recoveryAiService.analyze(
                            new RecoveryAiRequest(payment.getId())
                    );

            RecommendedAction finalAction =
                    recoveryPolicyService.determineAllowedAction(
                            payment,
                            aiResponse.recommendedAction()
                    );

            String result =
                    recoveryExecutionService.execute(
                            payment,
                            finalAction
                    );

            return "Webhook processed. Payment "
                    + payment.getId()
                    + ". AI action: "
                    + aiResponse.recommendedAction()
                    + ". Final action: "
                    + finalAction
                    + ". Result: "
                    + result;

        } catch (IllegalArgumentException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Failed to process Razorpay webhook: "
                            + exception.getMessage(),
                    exception
            );
        }
    }
}