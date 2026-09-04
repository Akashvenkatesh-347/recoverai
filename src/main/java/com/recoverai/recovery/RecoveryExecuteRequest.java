package com.recoverai.recovery;

import jakarta.validation.constraints.NotNull;

public record RecoveryExecuteRequest(

        @NotNull(message = "Recovery action is required")
        RecommendedAction action

) {
}
