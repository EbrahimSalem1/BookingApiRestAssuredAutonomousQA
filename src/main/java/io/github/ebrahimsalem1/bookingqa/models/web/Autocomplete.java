package io.github.ebrahimsalem1.bookingqa.models.web;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Contract of the booking.com search-box autocomplete ({@code POST /autocomplete.json}).
 * Unknown fields are ignored on purpose: the JSON schema checks the contract, the models
 * only carry what the tests reason about.
 */
public final class Autocomplete {

    private Autocomplete() {
    }

    public record Request(String query, String language, int size) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(List<Suggestion> results) {
        public List<Suggestion> resultsOrEmpty() {
            return results == null ? List.of() : results;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Suggestion(
            @JsonProperty("dest_id") String destId,
            @JsonProperty("dest_type") String destType,
            @JsonProperty("label") String label,
            @JsonProperty("cc1") String countryCode,
            @JsonProperty("latitude") Double latitude,
            @JsonProperty("longitude") Double longitude,
            @JsonProperty("nr_hotels") Integer hotelCount) {
    }
}
