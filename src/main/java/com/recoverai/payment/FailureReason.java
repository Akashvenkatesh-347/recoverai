package com.recoverai.payment;

public enum FailureReason {

    INSUFFICIENT_FUNDS,
    NETWORK_ERROR,
    TIMEOUT,
    CARD_EXPIRED,
    INVALID_CARD,
    PAYMENT_METHOD_DECLINED,
    UNKNOWN
}