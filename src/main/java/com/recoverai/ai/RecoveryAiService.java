package com.recoverai.ai;

import com.recoverai.exception.AiServiceException;
import com.recoverai.payment.Payment;
import com.recoverai.payment.repository.PaymentRepository;
import com.recoverai.recovery.RecommendedAction;
import com.recoverai.recovery.RecoveryPolicyService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class RecoveryAiService {

    private final ChatClient chatClient;
    private final PaymentRepository paymentRepository;
    private final RecoveryPolicyService recoveryPolicyService;

    public RecoveryAiService(
            ChatClient.Builder chatClientBuilder,
            PaymentRepository paymentRepository,
            RecoveryPolicyService recoveryPolicyService) {

        this.chatClient = chatClientBuilder.build();
        this.paymentRepository = paymentRepository;
        this.recoveryPolicyService = recoveryPolicyService;
    }

    public RecoveryAiResponse analyze(RecoveryAiRequest request) {

        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payment not found: " + request.paymentId()
                        )
                );

        if (payment.getSubscription() == null) {
            throw new IllegalArgumentException(
                    "Payment does not have an associated subscription"
            );
        }

        String prompt = """
                You are an AI payment recovery decision assistant.

                Analyze the following failed payment and subscription information.

                Payment amount: %s
                Currency: %s
                Payment status: %s
                Failure reason: %s
                Retry count: %d
                Subscription status: %s
                Plan name: %s

                Return your recommendation using exactly these rules:

                Risk level must be one of:
                LOW, MEDIUM, HIGH

                Recommended action must be one of:
                RETRY_PAYMENT, USER_NOTIFICATION, ESCALATE, NO_ACTION

                Confidence must be a number between 0 and 1.

                Provide a short reason explaining the recommendation.
                """.formatted(
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getRetryCount(),
                payment.getSubscription().getStatus(),
                payment.getSubscription().getPlanName()
        );

        try {

            RecoveryAiResponse aiResponse = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(RecoveryAiResponse.class);

            if (aiResponse == null) {
                throw new AiServiceException(
                        "AI service returned an empty response",
                        null
                );
            }

            RecommendedAction finalAction =
                    recoveryPolicyService.determineAllowedAction(
                            payment,
                            aiResponse.recommendedAction()
                    );

            return new RecoveryAiResponse(
                    aiResponse.riskLevel(),
                    aiResponse.reason(),
                    finalAction,
                    aiResponse.confidence()
            );

        } catch (AiServiceException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new AiServiceException(
                    "Failed to analyze payment recovery: "
                            + exception.getMessage(),
                    exception
            );
        }
    }
}