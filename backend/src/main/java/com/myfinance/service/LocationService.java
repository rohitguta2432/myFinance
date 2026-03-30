package com.myfinance.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LocationService {

    private Map<String, List<String>> statesAndCities = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        try {
            InputStream is = new ClassPathResource("india-states-cities.json").getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            statesAndCities = mapper.readValue(is, new TypeReference<LinkedHashMap<String, List<String>>>() {});
            log.info("✅ Loaded {} Indian states/UTs with cities from static resource", statesAndCities.size());
        } catch (Exception e) {
            log.error("Failed to load india-states-cities.json", e);
        }
    }

    public List<String> getStates() {
        return statesAndCities.keySet().stream().sorted().toList();
    }

    public List<String> getCities(String state) {
        if (state == null || state.isBlank()) return List.of();
        List<String> cities = statesAndCities.get(state.trim());
        return cities != null ? cities : List.of();
    }
}
