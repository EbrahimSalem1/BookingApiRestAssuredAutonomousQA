package io.github.ebrahimsalem1.bookingqa.bdd.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.github.ebrahimsalem1.bookingqa.bdd.context.ScenarioContext;
import io.github.ebrahimsalem1.bookingqa.clients.web.AutocompleteClient;
import io.github.ebrahimsalem1.bookingqa.clients.web.SearchResultsClient;
import io.github.ebrahimsalem1.bookingqa.models.web.Autocomplete;

import java.time.LocalDate;
import java.util.List;

import static io.github.ebrahimsalem1.bookingqa.core.guard.EnvironmentGuard.assumeNotBlocked;
import static org.assertj.core.api.Assertions.assertThat;

public class SearchSteps {

    private final ScenarioContext ctx;
    private final AutocompleteClient autocomplete = new AutocompleteClient();
    private final SearchResultsClient searchResults = new SearchResultsClient();

    public SearchSteps(ScenarioContext ctx) {
        this.ctx = ctx;
    }

    @Given("a stay of {int} nights for {int} adults starting {int} days from today")
    public void aStay(int nights, int adults, int daysAhead) {
        ctx.checkin = LocalDate.now().plusDays(daysAhead);
        ctx.checkout = ctx.checkin.plusDays(nights);
        ctx.adults = adults;
    }

    @When("I type {string} in the destination search box")
    public void iType(String query) {
        ctx.lastResponse = assumeNotBlocked(autocomplete.suggest(query));
        ctx.lastResponse.then().statusCode(200);
    }

    @Then("the top suggestion is {string} in {string}")
    public void topSuggestionIs(String city, String country) {
        List<Autocomplete.Suggestion> results = AutocompleteClient.parse(ctx.lastResponse).resultsOrEmpty();
        assertThat(results).as("suggestions").isNotEmpty();
        assertThat(results.get(0).label()).containsIgnoringCase(city).containsIgnoringCase(country);
    }

    @Then("every suggestion has a destination id, type and label")
    public void everySuggestionIsComplete() {
        assertThat(AutocompleteClient.parse(ctx.lastResponse).resultsOrEmpty()).allSatisfy(s -> {
            assertThat(s.destId()).as("dest_id").isNotBlank();
            assertThat(s.destType()).as("dest_type").isNotBlank();
            assertThat(s.label()).as("label").isNotBlank();
        });
    }

    @When("I search for {string}")
    public void iSearchFor(String destination) {
        var query = new SearchResultsClient.SearchQuery(destination, ctx.checkin, ctx.checkout, ctx.adults, 1, 0);
        ctx.lastResponse = assumeNotBlocked(searchResults.search(query));
    }

    @Then("the results page is served as HTML")
    public void resultsPageIsHtml() {
        assertThat(ctx.lastResponse.getStatusCode()).as("HTTP status").isEqualTo(200);
        assertThat(ctx.lastResponse.getContentType()).as("content type").startsWith("text/html");
    }

    @Then("the results page mentions {string}")
    public void resultsPageMentions(String text) {
        assertThat(ctx.lastResponse.getBody().asString()).containsIgnoringCase(text);
    }
}
