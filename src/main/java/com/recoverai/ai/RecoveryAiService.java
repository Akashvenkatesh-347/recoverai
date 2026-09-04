package com.recoverai.ai;

import com.recoverai.payment.Payment;
import com.recoverai.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class RecoveryAiService {

    private final PaymentRepository paymentRepository;
    private final RecoveryAiProvider recoveryAiProvider;

    public RecoveryAiService(
            PaymentRepository paymentRepository,
            RecoveryAiProvider recoveryAiProvider) {

        this.paymentRepository = paymentRepository;
        this.recoveryAiProvider = recoveryAiProvider;
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

        return recoveryAiProvider.recommend(payment);
    }
}