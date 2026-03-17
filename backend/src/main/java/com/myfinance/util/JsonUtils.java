package com.myfinance.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public final class JsonUtils {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtils() {}

    public static String toJson(Map<String, Integer> map) {
        if (map == null || map.isEmpty())
            return null;
        try {
            return MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("json.serialize.failed", e);
            return null;
        }
    }

    public static Map<String, Integer> fromJson(String json) {
        if (json == null || json.isBlank())
            return null;
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (JsonProcessingException e) {
            log.error("json.deserialize.failed", e);
            return null;
        }
    }
}
