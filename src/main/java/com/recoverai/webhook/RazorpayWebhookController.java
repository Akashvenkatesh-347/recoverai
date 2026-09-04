package com.recoverai.webhook;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/razorpay")
public class RazorpayWebhookController {

    private final RazorpayWebhookService razorpayWebhookService;
    private final WebhookEventRepository webhookEventRepository;
    private final RazorpayWebhookProcessingService
            razorpayWebhookProcessingService;

    public RazorpayWebhookController(
            RazorpayWebhookService razorpayWebhookService,
            WebhookEventRepository webhookEventRepository,
            RazorpayWebhookProcessingService
                    razorpayWebhookProcessingService) {

        this.razorpayWebhookService = razorpayWebhookService;
        this.webhookEventRepository = webhookEventRepository;
        this.razorpayWebhookProcessingService =
                razorpayWebhookProcessingService;
    }

    @PostMapping
    public ResponseEntity<String> receiveWebhook(
            @RequestBody String payload,
            @RequestHeader(
                    value = "X-Razorpay-Signature",
                    required = false
            )
            String signature,
            @RequestHeader(
                    value = "x-razorpay-event-id",
                    required = false
            )
            String eventId) {

        boolean valid =
                razorpayWebhookService.verifySignature(
                        payload,
                        signature
                );

        if (!valid) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid webhook signature");
        }

        if (eventId == null || eventId.isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body("Missing Razorpay event ID");
        }

        if (webhookEventRepository.existsByEventId(eventId)) {
            return ResponseEntity.ok(
                    "Webhook already processed"
            );
        }

        WebhookEvent webhookEvent = new WebhookEvent();
        webhookEvent.setEventId(eventId);

        webhookEventRepository.save(webhookEvent);

        String result =
                razorpayWebhookProcessingService.process(
                        payload
                );

        System.out.println("Received valid Razorpay webhook");
        System.out.println("Event ID: " + eventId);
        System.out.println("Result: " + result);

        return ResponseEntity.ok(result);
    }
}