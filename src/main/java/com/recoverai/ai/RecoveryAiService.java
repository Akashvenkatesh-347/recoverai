package com.recoverai.ai;

import com.recoverai.exception.AiServiceException;
import com.recoverai.recovery.RecommendedAction;
import com.recoverai.recovery.RiskLevel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class
RecoveryAiService {

    private final ChatClient chatClient;

    public RecoveryAiService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public RecoveryAiResponse analyze(RecoveryAiRequest request) {

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
                request.paymentAmount(),
                request.currency(),
                request.paymentStatus(),
                request.failureReason(),
                request.retryCount(),
                request.subscriptionStatus(),
                request.planName()
        );

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(RecoveryAiResponse.class);
        } catch (Exception e) {
            throw new AiServiceException(
                    "Failed to get a recovery recommendation from the AI service",
                    e
            );
        }
    }
}