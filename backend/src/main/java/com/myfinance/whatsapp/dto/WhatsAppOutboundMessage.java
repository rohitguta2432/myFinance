package com.myfinance.whatsapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound message DTO for sending messages via WhatsApp Cloud API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsAppOutboundMessage {

    @JsonProperty("messaging_product")
    @Builder.Default
    private String messagingProduct = "whatsapp";

    @JsonProperty("recipient_type")
    @Builder.Default
    private String recipientType = "individual";

    private String to;
    private String type;

    // Text message
    private TextBody text;

    // Template message
    private Template template;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextBody {
        @JsonProperty("preview_url")
        private boolean previewUrl;

        private String body;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Template {
        private String name;
        private Language language;
        private List<Component> components;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Language {
        private String code;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Component {
        private String type;
        private List<Parameter> parameters;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Parameter {
        private String type;
        private String text;
    }

    // ── Factory methods ─────────────────────────────────

    public static WhatsAppOutboundMessage text(String to, String body) {
        return WhatsAppOutboundMessage.builder()
                .to(to)
                .type("text")
                .text(TextBody.builder().body(body).build())
                .build();
    }

    public static WhatsAppOutboundMessage template(String to, String templateName, String languageCode) {
        return WhatsAppOutboundMessage.builder()
                .to(to)
                .type("template")
                .template(Template.builder()
                        .name(templateName)
                        .language(Language.builder().code(languageCode).build())
                        .build())
                .build();
    }
}
