package com.myfinance.whatsapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * WhatsApp Cloud API configuration from application.yml
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "whatsapp.api")
public class WhatsAppConfig {
    private String baseUrl;
    private String phoneNumberId;
    private String businessAccountId;
    private String accessToken;
    private String verifyToken;
}
