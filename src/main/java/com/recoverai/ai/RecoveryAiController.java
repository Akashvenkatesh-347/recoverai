package com.recoverai.ai;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class RecoveryAiController {

    private final RecoveryAiService recoveryAiService;

    public RecoveryAiController(RecoveryAiService recoveryAiService) {
        this.recoveryAiService = recoveryAiService;
    }

    @PostMapping("/recovery")
    public RecoveryAiResponse analyzeRecovery(
            @Valid @RequestBody RecoveryAiRequest request) {

        return recoveryAiService.analyze(request);
    }
}