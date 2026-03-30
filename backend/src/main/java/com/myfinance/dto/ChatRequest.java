package com.myfinance.dto;

import java.util.List;
import java.util.Map;

public record ChatRequest(String message, List<ChatMessage> history, Map<String, Object> financialContext) {}
