package io.github.ebrahimsalem1.bookingqa.tests.demand;

import io.github.ebrahimsalem1.bookingqa.base.BaseApiTest;
import io.github.ebrahimsalem1.bookingqa.clients.demand.DemandApiClient;
import io.github.ebrahimsalem1.bookingqa.core.guard.EnvironmentGuard;
import io.github.ebrahimsalem1.bookingqa.data.TestData;
import io.github.ebrahimsalem1.bookingqa.models.demand.DemandRequests;
import io.github.ebrahimsalem1.bookingqa.models.demand.DemandResponses;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Accommodation search through the official Booking.com Demand API. Needs partner credentials:
 * runs fully in {@code mock}, and in {@code sandbox}/{@code live} when DEMAND_TOKEN and
 * DEMAND_AFFILIATE_ID are set; otherwise every check is skipped as [NO_CREDENTIALS].
 */
@Epic("Search")
@Feature("Demand API accommodation search")
@Test(groups = {"demand-partner", "search"})
public class DemandSearchTest extends BaseApiTest {

    private DemandApiClient demand;

    @BeforeMethod(alwaysRun = true)
    public void requireCredentials() {
        EnvironmentGuard.requireDemandCredentials();
        demand = DemandApiClient.authenticated();
    }

    @Test(groups = {"smoke", "contract"}, description = "City search returns priced accommodations in the published contract")
    @Severity(SeverityLevel.BLOCKER)
    public void citySearchReturnsPricedAccommodations() {
        Response r = demand.searchAccommodations(DemandFixtures.search("Cairo"));
        r.then().statusCode(200).contentType(ContentType.JSON).body(matchesSchema("demand/search-response.json"));

        DemandResponses.Search search = r.as(DemandResponses.Search.class);
        assertThat(search.requestId()).as("request_id for support traceability").isNotBlank();
        assertThat(search.dataOrEmpty()).as("accommodations for Cairo").isNotEmpty();
        assertThat(search.dataOrEmpty()).allSatisfy(a -> {
            assertThat(a.id()).as("accommodation id").isPositive();
            assertThat(a.price().total().bookerCurrency()).as("total price of %s", a.id()).isPositive();
            assertThat(a.currency().booker()).as("booker currency of %s", a.id()).isEqualTo(config.currency());
        });
    }

    @Test(description = "Total price is never lower than the base price (taxes/charges only add)")
    @Severity(SeverityLevel.CRITICAL)
    public void totalPriceIsNeverBelowBasePrice() {
        DemandResponses.Search search = demand.searchAccommodations(DemandFixtures.search("Cairo"))
                .then().statusCode(200).extract().as(DemandResponses.Search.class);

        for (DemandResponses.Accommodation a : search.dataOrEmpty()) {
            BigDecimal base = a.price().base().bookerCurrency();
            BigDecimal total = a.price().total().bookerCurrency();
            assertThat(total).as("accommodation %s total vs base", a.id()).isGreaterThanOrEqualTo(base);
        }
    }

    @Test(description = "Next page token returns a different page of results")
    public void paginationReturnsTheNextPage() {
        DemandResponses.Search first = demand.searchAccommodations(DemandFixtures.search("Cairo"))
                .then().statusCode(200).extract().as(DemandResponses.Search.class);
        String nextPage = first.metadata() == null ? null : first.metadata().nextPage();
        assertThat(nextPage).as("metadata.next_page for a busy city").isNotBlank();

        DemandRequests.AccommodationSearch base = DemandFixtures.search("Cairo");
        DemandRequests.AccommodationSearch page2 = new DemandRequests.AccommodationSearch(base.booker(), base.checkin(),
                base.checkout(), base.city(), base.guests(), base.currency(), base.extras(), nextPage);
        DemandResponses.Search second = demand.searchAccommodations(page2)
                .then().statusCode(200).extract().as(DemandResponses.Search.class);

        List<Long> firstIds = first.dataOrEmpty().stream().map(DemandResponses.Accommodation::id).toList();
        assertThat(second.dataOrEmpty()).as("page 2").isNotEmpty();
        assertThat(second.dataOrEmpty()).extracting(DemandResponses.Accommodation::id)
                .as("page 2 must not repeat page 1").doesNotContainAnyElementsOf(firstIds);
    }

    @Test(groups = "negative", description = "Checkout before check-in is rejected with a validation error")
    public void checkoutBeforeCheckinIsRejected() {
        var body = DemandFixtures.search(TestData.destination("Cairo").demandCityId(), TestData.checkin(), TestData.checkin().minusDays(1));
        assertValidationError(demand.searchAccommodations(body));
    }

    @Test(groups = "negative", description = "Missing booker context is rejected with a validation error")
    public void missingBookerIsRejected() {
        var valid = DemandFixtures.search("Cairo");
        var body = new DemandRequests.AccommodationSearch(null, valid.checkin(), valid.checkout(), valid.city(),
                valid.guests(), valid.currency(), valid.extras(), null);
        assertValidationError(demand.searchAccommodations(body));
    }

    static void assertValidationError(Response r) {
        r.then().statusCode(400).body(matchesSchema("demand/error-response.json"));
        DemandResponses.Error error = r.as(DemandResponses.Error.class);
        assertThat(error.errorsOrEmpty()).as("errors[]").isNotEmpty();
        assertThat(error.errorsOrEmpty().get(0).id()).as("machine readable error id").isNotBlank();
        assertThat(error.errorsOrEmpty().get(0).message()).as("human readable message").isNotBlank();
    }
}
