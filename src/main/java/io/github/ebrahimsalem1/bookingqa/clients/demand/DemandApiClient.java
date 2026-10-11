package io.github.ebrahimsalem1.bookingqa.clients.demand;

import io.github.ebrahimsalem1.bookingqa.core.http.SpecFactory;
import io.github.ebrahimsalem1.bookingqa.models.demand.DemandRequests;
import io.qameta.allure.Allure;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import static io.restassured.RestAssured.given;

/**
 * Booking.com Demand API v3.1: the official partner API behind search, availability and
 * checkout (orders). Every operation is an HTTPS POST with a JSON body.
 */
public class DemandApiClient {

    public static final String SEARCH = "/accommodations/search";
    public static final String AVAILABILITY = "/accommodations/availability";
    public static final String ORDER_PREVIEW = "/orders/preview";

    private final RequestSpecification spec;
    private final String label;

    private DemandApiClient(RequestSpecification spec, String label) {
        this.spec = spec;
        this.label = label;
    }

    /** Uses configured credentials (env vars / CI secrets / mock token). */
    public static DemandApiClient authenticated() {
        return new DemandApiClient(SpecFactory.demand(), "auth");
    }

    /** No Authorization and no X-Affiliate-Id header at all. */
    public static DemandApiClient anonymous() {
        return new DemandApiClient(SpecFactory.demandAnonymous(), "anonymous");
    }

    /** Explicit credentials, e.g. a deliberately invalid token for negative checks. */
    public static DemandApiClient withCredentials(String token, String affiliateId) {
        return new DemandApiClient(SpecFactory.demand(token, affiliateId), "custom-credentials");
    }

    public Response searchAccommodations(DemandRequests.AccommodationSearch body) {
        return post(SEARCH, body);
    }

    public Response availability(DemandRequests.Availability body) {
        return post(AVAILABILITY, body);
    }

    public Response previewOrder(DemandRequests.OrderPreview body) {
        return post(ORDER_PREVIEW, body);
    }

    public Response post(String path, Object body) {
        return Allure.step("Demand API POST " + path + " [" + label + "]", () -> given()
                .spec(spec)
                .body(body)
                .when()
                .post(path)
                .andReturn());
    }

    public Response get(String path) {
        return Allure.step("Demand API GET " + path + " [" + label + "]", () -> given()
                .spec(spec)
                .when()
                .get(path)
                .andReturn());
    }
}
