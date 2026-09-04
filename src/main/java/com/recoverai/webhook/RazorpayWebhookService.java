package com.recoverai.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class RazorpayWebhookService {

    private final String webhookSecret;

    public RazorpayWebhookService(
            @Value("${razorpay.webhook.secret}") String webhookSecret) {

        this.webhookSecret = webhookSecret;
    }

    public boolean verifySignature(String payload, String signature) {

        if (payload == null || signature == null) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    webhookSecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] expectedSignature =
                    mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            byte[] receivedSignature =
                    hexToBytes(signature);

            return MessageDigest.isEqual(
                    expectedSignature,
                    receivedSignature
            );

        } catch (Exception exception) {
            return false;
        }
    }

    private byte[] hexToBytes(String hex) {

        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException(
                    "Invalid hexadecimal signature"
            );
        }

        byte[] bytes = new byte[hex.length() / 2];

        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] =
                    (byte) Integer.parseInt(
                            hex.substring(i, i + 2),
                            16
                    );
        }

        return bytes;
    }
}