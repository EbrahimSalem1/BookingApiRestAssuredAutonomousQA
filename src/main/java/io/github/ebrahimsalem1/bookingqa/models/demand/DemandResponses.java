package io.github.ebrahimsalem1.bookingqa.models.demand;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response bodies of the Booking.com Demand API v3.1. Only fields the checks reason about are
 * modelled; the JSON schemas in {@code src/test/resources/schemas/demand} own the full contract.
 */
public final class DemandResponses {

    private DemandResponses() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Search(@JsonProperty("request_id") String requestId,
                         List<Accommodation> data,
                         Metadata metadata) {
        public List<Accommodation> dataOrEmpty() {
            return data == null ? List.of() : data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metadata(@JsonProperty("next_page") String nextPage,
                           @JsonProperty("total_results") Integer totalResults) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Accommodation(Long id,
                                Currency currency,
                                Price price,
                                List<Product> products,
                                String url,
                                @JsonProperty("deep_link_url") String deepLinkUrl) {
        public List<Product> productsOrEmpty() {
            return products == null ? List.of() : products;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Currency(String accommodation, String booker) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Price(Money base, Money display, Money total, Money book) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Money(@JsonProperty("accommodation_currency") BigDecimal accommodationCurrency,
                        @JsonProperty("booker_currency") BigDecimal bookerCurrency) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(String id,
                          @JsonProperty("number_of_adults") Integer adults,
                          Price price,
                          Policies policies) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Policies(Cancellation cancellation, @JsonProperty("meal_plan") String mealPlan) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cancellation(String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Availability(@JsonProperty("request_id") String requestId, Accommodation data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Error(@JsonProperty("request_id") String requestId, List<ErrorItem> errors) {
        public List<ErrorItem> errorsOrEmpty() {
            return errors == null ? List.of() : errors;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorItem(String id, String message) {
    }
}
