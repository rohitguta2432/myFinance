package com.myfinance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myfinance.dto.ChatMessage;
import com.myfinance.dto.ChatRequest;
import com.myfinance.dto.ChatResponse;
import com.myfinance.service.BedrockChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BedrockChatService chatService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("POST /api/v1/chat - sends message and gets AI response")
    void chat_success() throws Exception {
        ChatRequest request = new ChatRequest(
                "How should I invest my surplus?",
                List.of(new ChatMessage("user", "Hello"), new ChatMessage("assistant", "Hi there!")),
                Map.of("monthlySurplus", 50000));

        ChatResponse response = new ChatResponse(
                "Based on your surplus of 50k, I recommend a mix of equity and debt funds.",
                "2026-03-28T10:30:00");

        when(chatService.chat(eq(1L), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Based on your surplus of 50k, I recommend a mix of equity and debt funds."))
                .andExpect(jsonPath("$.timestamp").value("2026-03-28T10:30:00"));

        verify(chatService).chat(eq(1L), any(ChatRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/chat - message with no history")
    void chat_noHistory() throws Exception {
        ChatRequest request = new ChatRequest(
                "What is SIP?",
                List.of(),
                null);

        ChatResponse response = new ChatResponse(
                "SIP stands for Systematic Investment Plan.",
                "2026-03-28T11:00:00");

        when(chatService.chat(eq(1L), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("SIP stands for Systematic Investment Plan."));
    }

    @Test
    @DisplayName("POST /api/v1/chat - missing header defaults to 0")
    void chat_missingHeader() throws Exception {
        ChatRequest request = new ChatRequest("Hello", List.of(), null);
        ChatResponse response = new ChatResponse("Hi!", "2026-03-28T12:00:00");

        when(chatService.chat(eq(0L), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(chatService).chat(eq(0L), any(ChatRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/chat - service exception propagates")
    void chat_serviceException() {
        when(chatService.chat(anyLong(), any(ChatRequest.class)))
                .thenThrow(new RuntimeException("Bedrock API error"));

        ChatRequest request = new ChatRequest("Hello", List.of(), null);

        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v1/chat")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))));
    }

    @Test
    @DisplayName("POST /api/v1/chat - empty body returns 400")
    void chat_emptyBody() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/chat - request with financial context")
    void chat_withFinancialContext() throws Exception {
        ChatRequest request = new ChatRequest(
                "Am I saving enough?",
                List.of(),
                Map.of(
                        "monthlyIncome", 150000,
                        "monthlyExpenses", 80000,
                        "savingsRate", 47));

        ChatResponse response = new ChatResponse(
                "Your 47% savings rate is excellent. You're well above the recommended 20%.",
                "2026-03-28T14:00:00");

        when(chatService.chat(eq(1L), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").isNotEmpty());
    }
}
