package com.myfinance.whatsapp.client;

import com.myfinance.whatsapp.config.WhatsAppConfig;
import com.myfinance.whatsapp.dto.WhatsAppOutboundMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * REST client for the Meta WhatsApp Cloud API (Graph API v22.0).
 */
@Slf4j
@Component
public class WhatsAppCloudApiClient {

    private final WhatsAppConfig config;
    private final RestClient restClient;

    public WhatsAppCloudApiClient(WhatsAppConfig config) {
        this.config = config;
        this.restClient = RestClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getAccessToken())
                .build();
    }

    /**
     * Send a message via the WhatsApp Cloud API.
     *
     * POST /{phone-number-id}/messages
     */
    public Map<String, Object> sendMessage(WhatsAppOutboundMessage message) {
        String url = "/" + config.getPhoneNumberId() + "/messages";
        log.info("whatsapp.api.send to={} type={}", message.getTo(), message.getType());

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(message)
                    .retrieve()
                    .body(Map.class);

            log.info("whatsapp.api.send.success to={} response={}", message.getTo(), response);
            return response;
        } catch (Exception e) {
            log.error("whatsapp.api.send.failed to={} error={}", message.getTo(), e.getMessage());
            throw e;
        }
    }

    /**
     * Send a plain text message.
     */
    public Map<String, Object> sendTextMessage(String to, String text) {
        return sendMessage(WhatsAppOutboundMessage.text(to, text));
    }

    /**
     * Send a template message (e.g., hello_world).
     */
    public Map<String, Object> sendTemplateMessage(String to, String templateName, String languageCode) {
        return sendMessage(WhatsAppOutboundMessage.template(to, templateName, languageCode));
    }
}
