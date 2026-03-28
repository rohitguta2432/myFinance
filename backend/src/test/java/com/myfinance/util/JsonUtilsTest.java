package com.myfinance.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

    // ------------------------------------------------------------------ toJson
    @Nested
    @DisplayName("toJson")
    class ToJson {

        @Test
        @DisplayName("serialises a single-entry map")
        void singleEntry() {
            Map<String, Integer> map = Map.of("salary", 50000);
            String json = JsonUtils.toJson(map);

            assertThat(json).isNotNull();
            assertThat(json).contains("\"salary\"");
            assertThat(json).contains("50000");
        }

        @Test
        @DisplayName("serialises a multi-entry map")
        void multipleEntries() {
            // Use LinkedHashMap to guarantee insertion order for predictable JSON
            Map<String, Integer> map = new LinkedHashMap<>();
            map.put("salary", 50000);
            map.put("rent", 12000);
            map.put("food", 5000);

            String json = JsonUtils.toJson(map);

            assertThat(json).isNotNull();
            assertThat(json).isEqualTo("{\"salary\":50000,\"rent\":12000,\"food\":5000}");
        }

        @Test
        @DisplayName("returns null for null map")
        void nullMap() {
            assertThat(JsonUtils.toJson(null)).isNull();
        }

        @Test
        @DisplayName("returns null for empty map")
        void emptyMap() {
            assertThat(JsonUtils.toJson(Collections.emptyMap())).isNull();
        }

        @Test
        @DisplayName("handles map with zero values")
        void zeroValues() {
            Map<String, Integer> map = Map.of("balance", 0);
            String json = JsonUtils.toJson(map);

            assertThat(json).isEqualTo("{\"balance\":0}");
        }

        @Test
        @DisplayName("handles map with negative values")
        void negativeValues() {
            Map<String, Integer> map = Map.of("loss", -1000);
            String json = JsonUtils.toJson(map);

            assertThat(json).contains("-1000");
        }

        @Test
        @DisplayName("handles map with large integer values")
        void largeValues() {
            Map<String, Integer> map = Map.of("netWorth", Integer.MAX_VALUE);
            String json = JsonUtils.toJson(map);

            assertThat(json).contains(String.valueOf(Integer.MAX_VALUE));
        }
    }

    // ------------------------------------------------------------------ fromJson
    @Nested
    @DisplayName("fromJson")
    class FromJson {

        @Test
        @DisplayName("deserialises valid JSON to map")
        void validJson() {
            String json = "{\"salary\":50000,\"rent\":12000}";
            Map<String, Integer> result = JsonUtils.fromJson(json);

            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result).containsEntry("salary", 50000);
            assertThat(result).containsEntry("rent", 12000);
        }

        @Test
        @DisplayName("deserialises single-entry JSON")
        void singleEntry() {
            Map<String, Integer> result = JsonUtils.fromJson("{\"balance\":0}");

            assertThat(result).isNotNull();
            assertThat(result).containsExactlyEntriesOf(Map.of("balance", 0));
        }

        @Test
        @DisplayName("returns null for null input")
        void nullInput() {
            assertThat(JsonUtils.fromJson(null)).isNull();
        }

        @Test
        @DisplayName("returns null for empty string")
        void emptyString() {
            assertThat(JsonUtils.fromJson("")).isNull();
        }

        @Test
        @DisplayName("returns null for blank (whitespace-only) string")
        void blankString() {
            assertThat(JsonUtils.fromJson("   ")).isNull();
        }

        @Test
        @DisplayName("returns null for malformed JSON")
        void malformedJson() {
            assertThat(JsonUtils.fromJson("{not valid json}")).isNull();
        }

        @Test
        @DisplayName("returns null for JSON array instead of object")
        void jsonArray() {
            assertThat(JsonUtils.fromJson("[1, 2, 3]")).isNull();
        }

        @Test
        @DisplayName("returns null for plain string")
        void plainString() {
            assertThat(JsonUtils.fromJson("hello")).isNull();
        }

        @Test
        @DisplayName("handles negative values in JSON")
        void negativeValues() {
            Map<String, Integer> result = JsonUtils.fromJson("{\"loss\":-500}");

            assertThat(result).isNotNull();
            assertThat(result).containsEntry("loss", -500);
        }
    }

    // -------------------------------------------------------- round-trip
    @Nested
    @DisplayName("round-trip serialisation")
    class RoundTrip {

        @Test
        @DisplayName("toJson then fromJson preserves data")
        void roundTrip() {
            Map<String, Integer> original = new LinkedHashMap<>();
            original.put("income", 100000);
            original.put("expenses", 60000);
            original.put("savings", 40000);

            String json = JsonUtils.toJson(original);
            Map<String, Integer> restored = JsonUtils.fromJson(json);

            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("round-trip with single entry")
        void roundTripSingle() {
            Map<String, Integer> original = Map.of("amount", 42);

            String json = JsonUtils.toJson(original);
            Map<String, Integer> restored = JsonUtils.fromJson(json);

            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("null map round-trips as null")
        void nullRoundTrip() {
            String json = JsonUtils.toJson(null);
            Map<String, Integer> result = JsonUtils.fromJson(json);

            assertThat(json).isNull();
            assertThat(result).isNull();
        }
    }
}
