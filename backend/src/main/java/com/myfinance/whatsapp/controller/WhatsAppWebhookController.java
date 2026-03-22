package com.myfinance.whatsapp.controller;

import com.myfinance.whatsapp.dto.WebhookPayload;
import com.myfinance.whatsapp.service.WhatsAppWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * WhatsApp Cloud API Webhook Controller.
 *
 * GET  /api/v1/whatsapp/webhook — Meta verification challenge
 * POST /api/v1/whatsapp/webhook — Incoming messages & status updates
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/whatsapp/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final WhatsAppWebhookService webhookService;

    /**
     * Webhook verification — Meta sends a GET request with a challenge.
     * Must return the challenge string with 200 OK to confirm.
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        String result = webhookService.verifyWebhook(mode, token, challenge);
        if (result != null) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    /**
     * Receive incoming webhook events (messages, status updates).
     * Always return 200 OK quickly — Meta retries on non-200 responses.
     */
    @PostMapping
    public ResponseEntity<String> receiveWebhook(@RequestBody WebhookPayload payload) {
        log.info("whatsapp.webhook.receive object={}", payload.getObject());

        // Process asynchronously in the future; synchronous for Phase 1
        webhookService.processWebhook(payload);

        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
