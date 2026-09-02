package com.recoverai.payment;

public enum FailureReason {

    INSUFFICIENT_FUNDS(true),
    NETWORK_ERROR(true),
    TIMEOUT(true),

    CARD_EXPIRED(false),
    INVALID_CARD(false),
    PAYMENT_METHOD_DECLINED(false),

    UNKNOWN(false);

    private final boolean retryable;

    FailureReason(boolean retryable) {
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}