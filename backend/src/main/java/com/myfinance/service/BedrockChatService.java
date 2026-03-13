package com.myfinance.service;

import com.myfinance.dto.ChatMessage;
import com.myfinance.dto.ChatRequest;
import com.myfinance.dto.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class BedrockChatService {

    @Value("${aws.bedrock.region:us-east-1}")
    private String awsRegion;

    @Value("${aws.bedrock.model-id:amazon.nova-lite-v1:0}")
    private String modelId;

    private BedrockRuntimeClient bedrockClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        this.bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
        log.info("✅ Bedrock client initialized — region={}, model={}", awsRegion, modelId);
    }

    public ChatResponse chat(ChatRequest request) {
        try {
            String systemPrompt = buildSystemPrompt(request.financialContext());
            String requestBody = buildRequestBody(systemPrompt, request.message(), request.history());

            InvokeModelResponse response = bedrockClient.invokeModel(
                    InvokeModelRequest.builder()
                            .modelId(modelId)
                            .contentType("application/json")
                            .accept("application/json")
                            .body(SdkBytes.fromUtf8String(requestBody))
                            .build()
            );

            String responseBody = response.body().asUtf8String();
            String reply = extractReply(responseBody);

            return new ChatResponse(reply, Instant.now().toString());

        } catch (Exception e) {
            log.error("❌ Bedrock invocation failed: {}", e.getMessage(), e);
            return new ChatResponse(
                    "Sorry, I'm unable to respond right now. Please try again in a moment. 🙏",
                    Instant.now().toString()
            );
        }
    }

    private String buildSystemPrompt(Map<String, Object> financialContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                You are **Kira** — a friendly, expert personal financial advisor for Indian users.

                ## Your Personality
                - Warm, approachable, and non-judgmental
                - Always respond in clear, professional English
                - Use emojis sparingly for warmth (💡🎯📊✅)
                - Always give actionable, specific advice — never vague platitudes
                - Reference Indian financial instruments: PPF, NPS, ELSS, SGB, FD, RD, EPF
                - Know Indian tax laws: 80C, 80D, 80CCD, HRA, Old vs New regime
                - Keep responses concise (3-5 short paragraphs max)
                - Use bullet points for clarity
                - If you are unsure, say so honestly

                ## Rules
                - NEVER recommend specific company stocks or mutual fund schemes by name
                - NEVER promise guaranteed returns
                - Always mention risk where applicable
                - Encourage professional consultation for complex decisions
                - Be aware of current Indian market context
                """);

        if (financialContext != null && !financialContext.isEmpty()) {
            sb.append("\n## User's Financial Profile (CONFIDENTIAL — use to personalize advice)\n");
            financialContext.forEach((key, value) -> {
                String readableKey = key.replaceAll("([A-Z])", " $1").trim();
                sb.append("- **").append(readableKey).append("**: ").append(value).append("\n");
            });
        }

        return sb.toString();
    }

    private boolean isNovaModel() {
        return modelId != null && modelId.contains("nova");
    }

    private String buildRequestBody(String systemPrompt, String userMessage, List<ChatMessage> history) {
        try {
            ObjectNode root = objectMapper.createObjectNode();

            if (isNovaModel()) {
                // Amazon Nova format
                root.put("schemaVersion", "messages-v1");

                // System prompt as array
                ArrayNode systemArr = objectMapper.createArrayNode();
                ObjectNode sysNode = objectMapper.createObjectNode();
                sysNode.put("text", systemPrompt);
                systemArr.add(sysNode);
                root.set("system", systemArr);

                // Inference config
                ObjectNode inferenceConfig = objectMapper.createObjectNode();
                inferenceConfig.put("max_new_tokens", 1024);
                inferenceConfig.put("temperature", 0.7);
                inferenceConfig.put("top_p", 0.9);
                root.set("inferenceConfig", inferenceConfig);
            } else {
                // Claude (Anthropic) format
                root.put("anthropic_version", "bedrock-2023-05-31");
                root.put("max_tokens", 1024);
                root.put("temperature", 0.7);
                root.put("system", systemPrompt);
            }

            ArrayNode messages = objectMapper.createArrayNode();
            boolean nova = isNovaModel();

            // Add conversation history (last 10 messages)
            if (history != null) {
                List<ChatMessage> recent = history.size() > 10 ? history.subList(history.size() - 10, history.size()) : history;
                for (ChatMessage msg : recent) {
                    ObjectNode msgNode = objectMapper.createObjectNode();
                    msgNode.put("role", msg.role());
                    ArrayNode contentArr = objectMapper.createArrayNode();
                    ObjectNode textNode = objectMapper.createObjectNode();
                    if (!nova) textNode.put("type", "text");
                    textNode.put("text", msg.content());
                    contentArr.add(textNode);
                    msgNode.set("content", contentArr);
                    messages.add(msgNode);
                }
            }

            // Add current user message
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            ArrayNode contentArr = objectMapper.createArrayNode();
            ObjectNode textNode = objectMapper.createObjectNode();
            if (!nova) textNode.put("type", "text");
            textNode.put("text", userMessage);
            contentArr.add(textNode);
            userMsg.set("content", contentArr);
            messages.add(userMsg);

            root.set("messages", messages);

            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Bedrock request body", e);
        }
    }

    private String extractReply(String responseBody) {
        try {
            var tree = objectMapper.readTree(responseBody);

            if (isNovaModel()) {
                // Nova format: { "output": { "message": { "content": [{"text": "..."}], "role": "assistant" } } }
                var output = tree.get("output");
                if (output != null && output.has("message")) {
                    var content = output.get("message").get("content");
                    if (content != null && content.isArray() && !content.isEmpty()) {
                        return content.get(0).get("text").asText();
                    }
                }
            } else {
                // Claude format: { "content": [{"text": "..."}] }
                var content = tree.get("content");
                if (content != null && content.isArray() && !content.isEmpty()) {
                    return content.get(0).get("text").asText();
                }
            }

            return "I received your message but couldn't generate a proper response.";
        } catch (Exception e) {
            log.error("Failed to parse Bedrock response: {}", e.getMessage());
            return "Could not parse the AI response.";
        }
    }
}
