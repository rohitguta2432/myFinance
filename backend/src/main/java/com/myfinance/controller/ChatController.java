package com.myfinance.controller;

import com.myfinance.dto.ChatRequest;
import com.myfinance.dto.ChatResponse;
import com.myfinance.service.BedrockChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "AI Chat", description = "AI-powered financial advisor (AWS Bedrock)")
public class ChatController {

    private final BedrockChatService chatService;

    @PostMapping
    @Operation(summary = "Send message to AI advisor")
    public ResponseEntity<ChatResponse> chat(
            @RequestHeader(value = "X-User-Id", required = false, defaultValue = "0") Long userId,
            @RequestBody ChatRequest request) {
        ChatResponse response = chatService.chat(userId, request);
        return ResponseEntity.ok(response);
    }
}
