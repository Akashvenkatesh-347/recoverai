package com.recoverai.ai;

import jakarta.validation.constraints.NotNull;

public record RecoveryAiRequest(

        @NotNull(message = "Payment ID is required")
        Long paymentId

) {
}
