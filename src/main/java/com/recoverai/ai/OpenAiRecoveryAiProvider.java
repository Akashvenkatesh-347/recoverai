package com.recoverai.ai;

import com.recoverai.exception.AiServiceException;
import com.recoverai.payment.Payment;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@org.springframework.context.annotation.Profile("!mock-ai")
public class OpenAiRecoveryAiProvider implements RecoveryAiProvider {

    private final ChatClient chatClient;

    public OpenAiRecoveryAiProvider(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public RecoveryAiResponse recommend(Payment payment) {

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

                IMPORTANT:
                Your recommendation is only a suggestion.
                A separate policy layer will determine whether the action
                is actually allowed.
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

            RecoveryAiResponse response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .entity(RecoveryAiResponse.class);

            if (response == null) {
                throw new AiServiceException(
                        "AI service returned an empty response",
                        null
                );
            }

            return response;

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