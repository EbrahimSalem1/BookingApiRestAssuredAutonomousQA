package io.github.ebrahimsalem1.bookingqa.tests.web;

import io.github.ebrahimsalem1.bookingqa.base.BaseApiTest;
import io.github.ebrahimsalem1.bookingqa.clients.web.AutocompleteClient;
import io.github.ebrahimsalem1.bookingqa.data.TestData;
import io.github.ebrahimsalem1.bookingqa.models.web.Autocomplete;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static io.github.ebrahimsalem1.bookingqa.core.guard.EnvironmentGuard.assumeNotBlocked;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.lessThan;

/**
 * Destination autocomplete: the first API every booking journey calls. If it degrades, users
 * cannot even reach search results, so it carries the highest search risk.
 */
@Epic("Search")
@Feature("Destination autocomplete")
@Test(groups = {"autocomplete", "search"})
public class AutocompleteApiTest extends BaseApiTest {

    private final AutocompleteClient autocomplete = new AutocompleteClient();

    @Test(groups = {"smoke", "contract"}, description = "City query returns the city first, in the published contract")
    @Severity(SeverityLevel.BLOCKER)
    public void cityQueryReturnsTheCityFirst() {
        Response response = assumeNotBlocked(autocomplete.suggest("Cairo"));

        response.then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body(matchesSchema("web/autocomplete-response.json"));

        Autocomplete.Suggestion top = top(response, "Cairo");
        assertThat(top.label()).as("top suggestion label").containsIgnoringCase("Cairo");
        assertThat(top.destType()).as("top suggestion type").isEqualToIgnoringCase("city");
        assertThat(top.destId()).as("destination id").isNotBlank();
    }

    @DataProvider(name = "destinations")
    public Object[][] destinations() {
        return TestData.destinations().stream().map(d -> new Object[]{d}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "destinations", description = "Top suggestion is the right city in the right country")
    @Severity(SeverityLevel.CRITICAL)
    public void topSuggestionIsTheRightCityInTheRightCountry(TestData.Destination destination) {
        Response response = assumeNotBlocked(autocomplete.suggest(destination.name()));
        response.then().statusCode(200);

        Autocomplete.Suggestion top = top(response, destination.name());
        assertThat(top.label())
                .as("top suggestion for '%s'", destination.name())
                .containsIgnoringCase(destination.name())
                .containsIgnoringCase(destination.expectedInLabel());
    }

    @Test(description = "Case and surrounding whitespace do not change what the user gets")
    @Description("Users type ' cAiRo ' as often as 'Cairo'. Both must resolve to the same destination.")
    public void queryIsNormalised() {
        Response clean = assumeNotBlocked(autocomplete.suggest("Cairo"));
        Response messy = assumeNotBlocked(autocomplete.suggest("  cAiRo  "));

        String cleanTop = top(clean, "Cairo").destId();
        String messyTop = top(messy, "  cAiRo  ").destId();
        assertThat(messyTop).as("dest_id for a messy query").isEqualTo(cleanTop);
    }

    @Test(groups = "contract", description = "Never returns more suggestions than requested")
    public void resultCountHonoursRequestedSize() {
        Response response = assumeNotBlocked(autocomplete.suggest("Cairo", config.language(), 3));
        response.then().statusCode(200);
        assertThat(AutocompleteClient.parse(response).resultsOrEmpty()).hasSizeBetween(1, 3);
    }

    @Test(description = "Arabic-script query is understood (multilingual market)")
    public void arabicScriptQueryIsSupported() {
        Response response = assumeNotBlocked(autocomplete.suggest("القاهرة", "ar", 5));
        response.then().statusCode(200).contentType(ContentType.JSON);
        assertThat(AutocompleteClient.parse(response).resultsOrEmpty()).as("suggestions for Arabic 'Cairo'").isNotEmpty();
    }

    @Test(groups = "performance", description = "Suggestions arrive within the latency objective (typing feels instant)")
    public void respondsWithinLatencyObjective() {
        // warm-up call so DNS/TLS setup is not measured
        assumeNotBlocked(autocomplete.suggest("Paris"));
        Response response = assumeNotBlocked(autocomplete.suggest("Cairo"));
        response.then().statusCode(200).time(lessThan(config.apiSlaMs()));
    }

    @Test(groups = "security", description = "JSON API responses carry transport and sniffing protections")
    @Description("The HTML pages are bot-protected from CI networks, but the autocomplete API is not: it is the "
            + "surface where header hygiene can be verified live on every run.")
    public void apiResponsesCarrySecurityHeaders() {
        Response response = assumeNotBlocked(autocomplete.suggest("Cairo"));
        String hsts = response.getHeader("Strict-Transport-Security");
        assertThat(hsts).as("Strict-Transport-Security").isNotBlank().containsIgnoringCase("max-age=");
        long maxAge = Long.parseLong(hsts.replaceAll("(?i).*max-age=(\\d+).*", "$1"));
        assertThat(maxAge).as("HSTS max-age seconds").isGreaterThanOrEqualTo(31_536_000L);
        assertThat(response.getHeader("X-Content-Type-Options")).as("X-Content-Type-Options").isEqualToIgnoringCase("nosniff");
        String server = response.getHeader("Server");
        if (server != null) {
            assertThat(server).as("Server header must not disclose a version").doesNotContainPattern("\\d+\\.\\d+");
        }
    }

    @DataProvider(name = "edgeQueries")
    public Object[][] edgeQueries() {
        return new Object[][]{
                {"empty", ""},
                {"blank", "   "},
                {"gibberish", "zzqxjvwkq"},
                {"single char", "c"},
                {"very long", "a".repeat(512)},
                {"emoji", "🏖️ beach"},
        };
    }

    @Test(groups = "negative", dataProvider = "edgeQueries", description = "Edge-case queries never cause a server error")
    public void edgeCaseQueriesAreHandledGracefully(String label, String query) {
        Response response = assumeNotBlocked(autocomplete.suggest(query));
        assertThat(response.getStatusCode()).as("HTTP %s for %s query", response.getStatusCode(), label).isLessThan(500);
        if (response.getStatusCode() == 200) {
            response.then().contentType(ContentType.JSON).body(matchesSchema("web/autocomplete-response.json"));
        }
    }

    @Test(groups = "negative", description = "Malformed JSON body is rejected without a server error")
    public void malformedBodyIsRejectedGracefully() {
        Response response = assumeNotBlocked(autocomplete.suggestRaw("{\"query\": \"Cairo\""));
        assertThat(response.getStatusCode()).as("HTTP %s for truncated JSON", response.getStatusCode()).isLessThan(500);
    }

    @DataProvider(name = "injection")
    public Object[][] injection() {
        return new Object[][]{
                {"<script>alert(1)</script>"},
                {"' OR '1'='1"},
                {"Cairo\"; DROP TABLE hotels;--"},
                {"{{7*7}}${7*7}"},
                {"../../../../etc/passwd"},
        };
    }

    @Test(groups = {"security", "negative"}, dataProvider = "injection",
            description = "Injection payloads are treated as plain text (no 5xx, no reflected markup)")
    @Severity(SeverityLevel.CRITICAL)
    public void injectionPayloadsAreTreatedAsText(String payload) {
        Response response = assumeNotBlocked(autocomplete.suggest(payload));
        assertThat(response.getStatusCode()).as("HTTP status for payload %s", payload).isLessThan(500);
        if (response.getStatusCode() == 200) {
            assertThat(response.getContentType()).as("content type").startsWith("application/json");
            List<Autocomplete.Suggestion> results = AutocompleteClient.parse(response).resultsOrEmpty();
            assertThat(results).extracting(Autocomplete.Suggestion::label)
                    .as("labels must never echo executable markup")
                    .noneMatch(l -> l != null && l.toLowerCase().contains("<script"));
        }
    }

    private static Autocomplete.Suggestion top(Response response, String query) {
        List<Autocomplete.Suggestion> results = AutocompleteClient.parse(response).resultsOrEmpty();
        assertThat(results).as("suggestions for '%s'", query).isNotEmpty();
        return results.get(0);
    }
}
