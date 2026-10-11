package io.github.ebrahimsalem1.bookingqa.models.demand;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request bodies of the Booking.com Demand API v3.1 (official partner API).
 * Field names follow the public OpenAPI description at developers.booking.com.
 */
public final class DemandRequests {

    private DemandRequests() {
    }

    public record Booker(String country, String platform) {
        public static Booker desktop(String country) {
            return new Booker(country, "desktop");
        }
    }

    public record Guests(@JsonProperty("number_of_adults") int adults,
                         @JsonProperty("number_of_rooms") int rooms) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AccommodationSearch(Booker booker,
                                      String checkin,
                                      String checkout,
                                      Integer city,
                                      Guests guests,
                                      String currency,
                                      List<String> extras,
                                      String page) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Availability(Long accommodation,
                               Booker booker,
                               String checkin,
                               String checkout,
                               Guests guests,
                               String currency) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OrderPreview(Booker booker, String currency, OrderAccommodation accommodation) {
    }

    public record OrderAccommodation(Long id, String checkin, String checkout, List<ProductAllocation> products) {
    }

    public record ProductAllocation(String id, Allocation allocation) {
    }

    public record Allocation(@JsonProperty("number_of_adults") int adults, List<Integer> children) {
    }
}
