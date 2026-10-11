package io.github.ebrahimsalem1.bookingqa.tests.demand;

import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import io.github.ebrahimsalem1.bookingqa.data.TestData;
import io.github.ebrahimsalem1.bookingqa.models.demand.DemandRequests;

import java.time.LocalDate;
import java.util.List;

/** Builders for valid Demand API requests; tests mutate one field at a time for negatives. */
public final class DemandFixtures {

    private DemandFixtures() {
    }

    public static DemandRequests.Booker booker() {
        return DemandRequests.Booker.desktop(QaConfig.get().bookerCountry());
    }

    public static DemandRequests.Guests twoAdults() {
        return new DemandRequests.Guests(2, 1);
    }

    public static DemandRequests.AccommodationSearch search(String city) {
        return search(TestData.destination(city).demandCityId(), TestData.checkin(), TestData.checkout(3));
    }

    public static DemandRequests.AccommodationSearch search(int cityId, LocalDate checkin, LocalDate checkout) {
        return new DemandRequests.AccommodationSearch(booker(), checkin.toString(), checkout.toString(),
                cityId, twoAdults(), QaConfig.get().currency(), List.of("extra_charges", "products"), null);
    }

    public static DemandRequests.Availability availability(long accommodationId, LocalDate checkin, LocalDate checkout) {
        return new DemandRequests.Availability(accommodationId, booker(), checkin.toString(), checkout.toString(),
                twoAdults(), QaConfig.get().currency());
    }

    public static DemandRequests.OrderPreview preview(long accommodationId, String productId, LocalDate checkin, LocalDate checkout) {
        return new DemandRequests.OrderPreview(booker(), QaConfig.get().currency(),
                new DemandRequests.OrderAccommodation(accommodationId, checkin.toString(), checkout.toString(),
                        List.of(new DemandRequests.ProductAllocation(productId, new DemandRequests.Allocation(2, List.of())))));
    }
}
