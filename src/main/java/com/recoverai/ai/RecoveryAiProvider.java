package com.recoverai.ai;

import com.recoverai.payment.Payment;

public interface RecoveryAiProvider {

    RecoveryAiResponse recommend(Payment payment);
}