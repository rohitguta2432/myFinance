package com.myfinance.controller;

import com.myfinance.service.LocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
@Tag(name = "Location", description = "Indian states and cities lookup")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/states")
    @Operation(summary = "Get all Indian states and UTs")
    public ResponseEntity<List<String>> getStates() {
        return ResponseEntity.ok(locationService.getStates());
    }

    @GetMapping("/cities")
    @Operation(summary = "Get cities for a state")
    public ResponseEntity<List<String>> getCities(@RequestParam String state) {
        return ResponseEntity.ok(locationService.getCities(state));
    }
}
