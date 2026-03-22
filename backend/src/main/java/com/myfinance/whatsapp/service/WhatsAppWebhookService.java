package com.myfinance.whatsapp.service;

import com.myfinance.whatsapp.config.WhatsAppConfig;
import com.myfinance.whatsapp.dto.WebhookPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Processes incoming WhatsApp webhook events.
 * Handles verification challenges and routes incoming messages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppWebhookService {

    private final WhatsAppConfig config;
    private final WhatsAppMessageService messageService;

    /**
     * Handle Meta's webhook verification challenge.
     *
     * @return the challenge string if verify_token matches, null otherwise
     */
    public String verifyWebhook(String mode, String token, String challenge) {
        log.info("whatsapp.webhook.verify mode={}", mode);

        if ("subscribe".equals(mode) && config.getVerifyToken().equals(token)) {
            log.info("whatsapp.webhook.verify.success");
            return challenge;
        }

        log.warn("whatsapp.webhook.verify.failed token_match={}", config.getVerifyToken().equals(token));
        return null;
    }

    /**
     * Process an incoming webhook POST payload.
     */
    public void processWebhook(WebhookPayload payload) {
        if (payload == null || payload.getEntry() == null) {
            log.warn("whatsapp.webhook.receive.empty");
            return;
        }

        for (WebhookPayload.Entry entry : payload.getEntry()) {
            if (entry.getChanges() == null) continue;

            for (WebhookPayload.Change change : entry.getChanges()) {
                if (change.getValue() == null) continue;

                // Process messages
                List<WebhookPayload.Message> messages = change.getValue().getMessages();
                if (messages != null && !messages.isEmpty()) {
                    for (WebhookPayload.Message message : messages) {
                        processIncomingMessage(message, change.getValue().getContacts());
                    }
                }

                // Process delivery statuses (logging only for now)
                List<WebhookPayload.Status> statuses = change.getValue().getStatuses();
                if (statuses != null && !statuses.isEmpty()) {
                    for (WebhookPayload.Status status : statuses) {
                        log.info("whatsapp.webhook.status messageId={} status={} recipient={}",
                                status.getId(), status.getStatus(), status.getRecipientId());
                    }
                }
            }
        }
    }

    /**
     * Process a single incoming message.
     * Phase 1: Echo mode — replies with confirmation.
     * Phase 2 (future): Route to session state machine.
     */
    private void processIncomingMessage(WebhookPayload.Message message,
                                         List<WebhookPayload.Contact> contacts) {
        String from = message.getFrom();
        String type = message.getType();
        String contactName = (contacts != null && !contacts.isEmpty() && contacts.get(0).getProfile() != null)
                ? contacts.get(0).getProfile().getName()
                : "Unknown";

        log.info("whatsapp.webhook.message.receive from={} type={} name={}", from, type, contactName);

        switch (type) {
            case "text" -> {
                String body = message.getText() != null ? message.getText().getBody() : "";
                log.info("whatsapp.webhook.message.text from={} body=\"{}\"", from, body);

                // Phase 1: Echo mode for testing
                // Phase 2: Route to conversation state machine
                if (body.equalsIgnoreCase("hi") || body.equalsIgnoreCase("hello")
                        || body.equalsIgnoreCase("hey") || body.equalsIgnoreCase("menu")) {
                    messageService.sendWelcomeMenu(from);
                } else {
                    messageService.sendEcho(from, body);
                }
            }
            case "document" -> {
                log.info("whatsapp.webhook.message.document from={} filename={}",
                        from, message.getDocument() != null ? message.getDocument().getFilename() : "unknown");
                messageService.sendText(from, "📄 Document received! Form 16 parsing coming soon.");
            }
            case "image" -> {
                log.info("whatsapp.webhook.message.image from={}", from);
                messageService.sendText(from, "🖼️ Image received! Processing coming soon.");
            }
            case "interactive" -> {
                log.info("whatsapp.webhook.message.interactive from={}", from);
                // Handle button/list replies in Phase 2
                messageService.sendText(from, "Got your selection! Full flows coming soon.");
            }
            default -> {
                log.info("whatsapp.webhook.message.unsupported from={} type={}", from, type);
                messageService.sendText(from, "I can process text, documents, and images. Try sending a message!");
            }
        }
    }
}
