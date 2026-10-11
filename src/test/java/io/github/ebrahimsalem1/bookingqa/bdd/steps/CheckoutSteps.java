package io.github.ebrahimsalem1.bookingqa.bdd.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.ebrahimsalem1.bookingqa.bdd.context.ScenarioContext;
import io.github.ebrahimsalem1.bookingqa.clients.demand.DemandApiClient;
import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import io.github.ebrahimsalem1.bookingqa.core.guard.EnvironmentGuard;
import io.github.ebrahimsalem1.bookingqa.data.TestData;
import io.github.ebrahimsalem1.bookingqa.models.demand.DemandResponses;
import io.github.ebrahimsalem1.bookingqa.tests.demand.DemandFixtures;

import java.math.BigDecimal;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;

public class CheckoutSteps {

    private final ScenarioContext ctx;

    public CheckoutSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    private DemandApiClient partner() {
        EnvironmentGuard.requireDemandCredentials();
        return DemandApiClient.authenticated();
    }

    @When("I search accommodations in {string}")
    public void iSearchAccommodations(String city) {
        var body = DemandFixtures.search(TestData.destination(city).demandCityId(), ctx.checkin, ctx.checkout);
        DemandResponses.Search search = partner().searchAccommodations(body)
                .then().statusCode(200).extract().as(DemandResponses.Search.class);
        assertThat(search.dataOrEmpty()).as("accommodations in %s", city).isNotEmpty();
        ctx.accommodationId = search.dataOrEmpty().get(0).id();
    }

    @When("I check availability of the first accommodation found")
    public void iCheckAvailability() {
        DemandResponses.Availability availability = partner()
                .availability(DemandFixtures.availability(ctx.accommodationId, ctx.checkin, ctx.checkout))
                .then().statusCode(200).extract().as(DemandResponses.Availability.class);
        ctx.chosenProduct = availability.data().productsOrEmpty().stream()
                .min(Comparator.comparing(p -> p.price().total().bookerCurrency()))
                .orElseThrow(() -> new AssertionError("no bookable product"));
    }

    @When("I preview an order for its cheapest product")
    public void iPreviewAnOrder() {
        ctx.lastResponse = partner().previewOrder(
                DemandFixtures.preview(ctx.accommodationId, ctx.chosenProduct.id(), ctx.checkin, ctx.checkout));
        ctx.lastResponse.then().statusCode(200);
    }

    @Then("the order preview total equals the availability price")
    public void previewTotalEqualsAvailabilityPrice() {
        BigDecimal preview = new BigDecimal(ctx.lastResponse.jsonPath().getString("data.accommodation.price.total.booker_currency"));
        assertThat(preview).isEqualByComparingTo(ctx.chosenProduct.price().total().bookerCurrency());
    }

    @Then("the order preview is in the booker currency")
    public void previewIsInBookerCurrency() {
        assertThat(ctx.lastResponse.jsonPath().getString("data.currency.booker")).isEqualTo(QaConfig.get().currency());
    }

    @When("an anonymous client previews an order")
    public void anonymousPreview() {
        ctx.lastResponse = DemandApiClient.anonymous().previewOrder(
                DemandFixtures.preview(10004L, "1000401_95127794_2_0_0", TestData.checkin(), TestData.checkout(3)));
    }

    @Then("the request is rejected as unauthenticated")
    public void rejectedAsUnauthenticated() {
        assertThat(ctx.lastResponse.getStatusCode()).isIn(401, 403);
    }
}
