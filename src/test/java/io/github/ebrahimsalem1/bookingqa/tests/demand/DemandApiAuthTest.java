package io.github.ebrahimsalem1.bookingqa.tests.demand;

import io.github.ebrahimsalem1.bookingqa.base.BaseApiTest;
import io.github.ebrahimsalem1.bookingqa.clients.demand.DemandApiClient;
import io.github.ebrahimsalem1.bookingqa.data.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The authentication gate in front of search and checkout of the official Booking.com Demand API.
 * Runs against the real production host without any credentials: everything here must be refused.
 */
@Epic("Checkout")
@Feature("Demand API authentication gate")
@Test(groups = {"demand-auth", "checkout", "security", "negative"})
public class DemandApiAuthTest extends BaseApiTest {

    private static final String FAKE_TOKEN = "qa-invalid-token-7f3c9e";

    @Test(groups = "smoke", description = "Search without credentials is refused with 401")
    @Severity(SeverityLevel.BLOCKER)
    public void searchWithoutCredentialsIsRefused() {
        Response r = DemandApiClient.anonymous().searchAccommodations(DemandFixtures.search("Cairo"));
        assertRefused(r);
    }

    @Test(description = "Search with an invalid bearer token is refused")
    public void searchWithInvalidTokenIsRefused() {
        Response r = DemandApiClient.withCredentials(FAKE_TOKEN, "123456").searchAccommodations(DemandFixtures.search("Cairo"));
        assertRefused(r);
        assertThat(r.getBody().asString()).as("token must never be echoed back").doesNotContain(FAKE_TOKEN);
    }

    @Test(description = "Checkout (order preview) without credentials is refused")
    @Severity(SeverityLevel.CRITICAL)
    public void orderPreviewWithoutCredentialsIsRefused() {
        Response r = DemandApiClient.anonymous().previewOrder(
                DemandFixtures.preview(10004L, "1000401_95127794_2_0_0", TestData.checkin(), TestData.checkout(3)));
        assertRefused(r);
    }

    @Test(description = "Wrong HTTP method on a POST-only endpoint is neither served nor crashes")
    public void wrongHttpMethodIsRejected() {
        Response r = DemandApiClient.anonymous().get(DemandApiClient.SEARCH);
        assertThat(r.getStatusCode()).as("HTTP status for GET %s", DemandApiClient.SEARCH).isBetween(400, 499);
    }

    @Test(description = "Error responses do not leak implementation details")
    public void errorsDoNotLeakInternals() {
        Response r = DemandApiClient.withCredentials(FAKE_TOKEN, "not-a-number").searchAccommodations(DemandFixtures.search("Cairo"));
        String body = r.getBody().asString();
        assertThat(r.getStatusCode()).isLessThan(500);
        assertThat(body).as("error body")
                .doesNotContainIgnoringCase("stacktrace")
                .doesNotContainIgnoringCase("exception")
                .doesNotContain("at com.")
                .doesNotContain("at java.");
    }

    private static void assertRefused(Response r) {
        assertThat(r.getStatusCode()).as("HTTP status of unauthenticated call").isIn(401, 403);
        if (r.getContentType() != null && r.getContentType().contains("json")) {
            r.then().body(matchesSchema("demand/error-response.json"));
        }
    }
}
