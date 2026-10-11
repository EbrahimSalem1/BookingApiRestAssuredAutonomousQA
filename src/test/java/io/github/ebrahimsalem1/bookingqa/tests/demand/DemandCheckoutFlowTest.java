package io.github.ebrahimsalem1.bookingqa.tests.demand;

import io.github.ebrahimsalem1.bookingqa.base.BaseApiTest;
import io.github.ebrahimsalem1.bookingqa.clients.demand.DemandApiClient;
import io.github.ebrahimsalem1.bookingqa.core.guard.EnvironmentGuard;
import io.github.ebrahimsalem1.bookingqa.data.TestData;
import io.github.ebrahimsalem1.bookingqa.models.demand.DemandResponses;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checkout up to (not including) order creation: search -> availability -> order preview.
 * Orders are never created: this suite must be safe to point at production.
 */
@Epic("Checkout")
@Feature("Demand API checkout flow")
@Test(groups = {"demand-partner", "checkout"})
public class DemandCheckoutFlowTest extends BaseApiTest {

    private DemandApiClient demand;
    private final LocalDate checkin = TestData.checkin();
    private final LocalDate checkout = TestData.checkout(3);

    @BeforeMethod(alwaysRun = true)
    public void requireCredentials() {
        EnvironmentGuard.requireDemandCredentials();
        demand = DemandApiClient.authenticated();
    }

    @Test(groups = "smoke", description = "Price quoted at availability is the price previewed at checkout")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Price integrity across the funnel: what the traveller is shown before checkout must be what checkout charges.")
    public void availabilityPriceIsHonouredAtCheckoutPreview() {
        long accommodationId = Allure.step("Search Cairo and pick the first accommodation", () -> {
            DemandResponses.Search search = demand.searchAccommodations(DemandFixtures.search("Cairo"))
                    .then().statusCode(200).extract().as(DemandResponses.Search.class);
            assertThat(search.dataOrEmpty()).as("search results").isNotEmpty();
            return search.dataOrEmpty().get(0).id();
        });

        DemandResponses.Product cheapest = Allure.step("Check availability and pick the cheapest product", () -> {
            Response r = demand.availability(DemandFixtures.availability(accommodationId, checkin, checkout));
            r.then().statusCode(200).body(matchesSchema("demand/availability-response.json"));
            DemandResponses.Availability availability = r.as(DemandResponses.Availability.class);
            assertThat(availability.data().id()).as("availability is for the requested property").isEqualTo(accommodationId);
            return availability.data().productsOrEmpty().stream()
                    .min(Comparator.comparing(p -> p.price().total().bookerCurrency()))
                    .orElseThrow(() -> new AssertionError("No bookable product for accommodation " + accommodationId));
        });

        Response preview = Allure.step("Preview the order for product " + cheapest.id(), () ->
                demand.previewOrder(DemandFixtures.preview(accommodationId, cheapest.id(), checkin, checkout)));
        preview.then().statusCode(200).body(matchesSchema("demand/order-preview-response.json"));

        JsonPath json = preview.jsonPath();
        BigDecimal previewTotal = new BigDecimal(json.getString("data.accommodation.price.total.booker_currency"));
        assertThat(json.getLong("data.accommodation.id")).as("previewed accommodation").isEqualTo(accommodationId);
        assertThat(json.getString("data.accommodation.products[0].id")).as("previewed product").isEqualTo(cheapest.id());
        assertThat(previewTotal).as("checkout preview total vs availability total")
                .isEqualByComparingTo(cheapest.price().total().bookerCurrency());
        assertThat(json.getString("data.currency.booker")).as("checkout currency").isEqualTo(config.currency());
    }

    @Test(groups = "negative", description = "Previewing an unknown product is rejected, not silently priced")
    public void unknownProductIsRejectedAtPreview() {
        Response r = demand.previewOrder(DemandFixtures.preview(10004L, "unknown-product", checkin, checkout));
        assertThat(r.getStatusCode()).as("HTTP status for unknown product").isIn(400, 404, 409, 422);
        r.then().body(matchesSchema("demand/error-response.json"));
    }

    @Test(groups = "negative", description = "Availability for a past check-in date is rejected")
    public void pastCheckinIsRejectedAtAvailability() {
        LocalDate past = LocalDate.now().minusDays(10);
        DemandSearchTest.assertValidationError(demand.availability(DemandFixtures.availability(10004L, past, past.plusDays(2))));
    }
}
