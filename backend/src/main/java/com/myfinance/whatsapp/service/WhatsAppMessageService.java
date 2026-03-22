package com.myfinance.whatsapp.service;

import com.myfinance.whatsapp.client.WhatsAppCloudApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * High-level service for sending WhatsApp messages.
 * Abstracts the Cloud API client into business-friendly methods.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsAppMessageService {

    private final WhatsAppCloudApiClient cloudApiClient;

    /**
     * Send a plain text reply to a user.
     */
    public void sendText(String to, String text) {
        log.info("whatsapp.message.send.text to={} length={}", to, text.length());
        cloudApiClient.sendTextMessage(to, text);
    }

    /**
     * Send the welcome menu as a text message.
     * (Will be upgraded to interactive list message later)
     */
    public void sendWelcomeMenu(String to) {
        String menu = """
                🤖 Welcome to MyFinancial! 🎉
                I'm your personal financial advisor.
                
                What would you like to do?
                
                1️⃣ Quick Financial Health Check
                2️⃣ Upload Form 16 for Analysis
                3️⃣ Tax Savings Calculator
                4️⃣ File ITR
                
                Reply with a number (1-4) to get started!""";

        sendText(to, menu);
    }

    /**
     * Echo back received text for testing/debugging.
     */
    public void sendEcho(String to, String receivedText) {
        String echo = "✅ Received: \"" + receivedText + "\"\n\n"
                + "MyFinancial Bot is connected! 🎉\n"
                + "Full conversation flows coming soon.";
        sendText(to, echo);
    }
}
