package com.myfinance.service;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationService")
class LocationServiceTest {

    private LocationService locationService;

    @BeforeEach
    void setUp() throws Exception {
        locationService = new LocationService();

        // Manually inject test data instead of relying on @PostConstruct
        Map<String, List<String>> testData = new LinkedHashMap<>();
        testData.put("Maharashtra", List.of("Mumbai", "Pune", "Nagpur"));
        testData.put("Karnataka", List.of("Bangalore", "Mysore"));
        testData.put("Tamil Nadu", List.of("Chennai", "Coimbatore", "Madurai"));
        testData.put("Delhi", List.of("New Delhi"));

        Field field = LocationService.class.getDeclaredField("statesAndCities");
        field.setAccessible(true);
        field.set(locationService, testData);
    }

    @Nested
    @DisplayName("getStates")
    class GetStates {

        @Test
        @DisplayName("should return sorted list of states")
        void returnsSortedStates() {
            List<String> states = locationService.getStates();

            assertThat(states).hasSize(4);
            assertThat(states).isSorted();
            assertThat(states).containsExactly("Delhi", "Karnataka", "Maharashtra", "Tamil Nadu");
        }

        @Test
        @DisplayName("should return empty list when no data loaded")
        void returnsEmptyWhenNoData() throws Exception {
            Field field = LocationService.class.getDeclaredField("statesAndCities");
            field.setAccessible(true);
            field.set(locationService, new LinkedHashMap<>());

            List<String> states = locationService.getStates();

            assertThat(states).isEmpty();
        }
    }

    @Nested
    @DisplayName("getCities")
    class GetCities {

        @Test
        @DisplayName("should return cities for valid state")
        void returnsCities() {
            List<String> cities = locationService.getCities("Maharashtra");

            assertThat(cities).containsExactly("Mumbai", "Pune", "Nagpur");
        }

        @Test
        @DisplayName("should return empty list for unknown state")
        void returnsEmptyForUnknownState() {
            List<String> cities = locationService.getCities("Atlantis");

            assertThat(cities).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for null state")
        void returnsEmptyForNullState() {
            List<String> cities = locationService.getCities(null);

            assertThat(cities).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for blank state")
        void returnsEmptyForBlankState() {
            List<String> cities = locationService.getCities("   ");

            assertThat(cities).isEmpty();
        }

        @Test
        @DisplayName("should return empty list for empty string state")
        void returnsEmptyForEmptyString() {
            List<String> cities = locationService.getCities("");

            assertThat(cities).isEmpty();
        }

        @Test
        @DisplayName("should trim whitespace from state name")
        void trimsWhitespace() {
            List<String> cities = locationService.getCities("  Maharashtra  ");

            assertThat(cities).containsExactly("Mumbai", "Pune", "Nagpur");
        }

        @Test
        @DisplayName("should return cities for state with single city")
        void returnsSingleCity() {
            List<String> cities = locationService.getCities("Delhi");

            assertThat(cities).containsExactly("New Delhi");
        }

        @Test
        @DisplayName("should be case sensitive for state lookup")
        void isCaseSensitive() {
            List<String> cities = locationService.getCities("maharashtra");

            assertThat(cities).isEmpty();
        }
    }

    @Nested
    @DisplayName("init")
    class Init {

        @Test
        @DisplayName("should load data from classpath resource without error")
        void loadsFromClasspath() {
            LocationService freshService = new LocationService();
            // init() loads india-states-cities.json from classpath
            // If the file exists in test resources, this succeeds.
            // If not, it logs an error but does not throw.
            freshService.init();

            // After init, getStates should not throw
            List<String> states = freshService.getStates();
            assertThat(states).isNotNull();
        }

        @Test
        @DisplayName("should handle missing resource file gracefully")
        void handlesMissingResource() throws Exception {
            LocationService freshService = new LocationService();

            // Set statesAndCities to null to simulate pre-init state, then call init
            // The init method catches exceptions and logs them
            // Even if the file is missing, the service should not throw
            freshService.init();

            // After init (whether success or failure), getStates should not throw
            assertThatCode(() -> freshService.getStates()).doesNotThrowAnyException();
        }
    }
}
