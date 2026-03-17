package com.myfinance.controller;

import com.myfinance.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/states")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(locationService.getStates());
    }

    @GetMapping("/cities")
    public ResponseEntity<List<String>> getCities(@RequestParam String state) {
        return ResponseEntity.ok(locationService.getCities(state));
    }
}
