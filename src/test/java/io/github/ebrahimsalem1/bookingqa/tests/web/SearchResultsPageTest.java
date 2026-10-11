package io.github.ebrahimsalem1.bookingqa.tests.web;

import io.github.ebrahimsalem1.bookingqa.base.BaseApiTest;
import io.github.ebrahimsalem1.bookingqa.clients.web.SearchResultsClient;
import io.github.ebrahimsalem1.bookingqa.clients.web.SearchResultsClient.SearchQuery;
import io.github.ebrahimsalem1.bookingqa.data.TestData;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.restassured.response.Response;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.github.ebrahimsalem1.bookingqa.core.guard.EnvironmentGuard.assumeNotBlocked;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.startsWith;

/**
 * The server-rendered search results page ({@code /searchresults.html}) checked at HTTP level:
 * what the server returns for valid and invalid search criteria, independent of any browser.
 */
@Epic("Search")
@Feature("Search results page")
@Test(groups = {"web", "search"})
public class SearchResultsPageTest extends BaseApiTest {

    private final SearchResultsClient searchResults = new SearchResultsClient();

    @Test(groups = "smoke", description = "Valid search returns an HTML results page for the destination")
    @Severity(SeverityLevel.BLOCKER)
    public void validSearchReturnsResultsPage() {
        SearchQuery query = SearchQuery.of("Cairo", TestData.checkin(), 3, 2);
        Response response = assumeNotBlocked(searchResults.search(query));

        response.then()
                .statusCode(200)
                .contentType(startsWith("text/html"))
                .body(containsStringIgnoringCase("Cairo"));
    }

    @Test(groups = "performance", description = "Results page is served within the latency objective")
    public void resultsPageWithinLatencyObjective() {
        SearchQuery query = SearchQuery.of("Paris", TestData.checkin(), 2, 2);
        Response response = assumeNotBlocked(searchResults.search(query));
        response.then().statusCode(200).time(lessThan(config.webSlaMs()));
    }

    @DataProvider(name = "invalidCriteria")
    public Object[][] invalidCriteria() {
        LocalDate in = TestData.checkin();
        return new Object[][]{
                {"checkout before checkin", new SearchQuery("Cairo", in, in.minusDays(2), 2, 1, 0)},
                {"zero nights", new SearchQuery("Cairo", in, in, 2, 1, 0)},
                {"check-in in the past", new SearchQuery("Cairo", LocalDate.now().minusYears(1), LocalDate.now().minusYears(1).plusDays(2), 2, 1, 0)},
                {"zero adults", new SearchQuery("Cairo", in, in.plusDays(2), 0, 1, 0)},
                {"absurd party size", new SearchQuery("Cairo", in, in.plusDays(2), 999, 1, 0)},
                {"stay longer than a year", new SearchQuery("Cairo", in, in.plusDays(400), 2, 1, 0)},
                {"empty destination", new SearchQuery("", in, in.plusDays(2), 2, 1, 0)},
        };
    }

    @Test(groups = "negative", dataProvider = "invalidCriteria",
            description = "Invalid search criteria are handled without a server error")
    public void invalidCriteriaAreHandledGracefully(String label, SearchQuery query) {
        Response response = assumeNotBlocked(searchResults.search(query));
        assertThat(response.getStatusCode())
                .as("HTTP %s for '%s'", response.getStatusCode(), label)
                .isLessThan(500);
    }

    @Test(groups = "negative", description = "Malformed dates and unknown parameters never cause a server error")
    public void malformedParametersAreHandledGracefully() {
        Map<String, Object> params = new LinkedHashMap<>(SearchQuery.of("Cairo", TestData.checkin(), 2, 2).toParams());
        params.put("checkin", "2026-13-45");
        params.put("checkout", "not-a-date");
        params.put("group_adults", "two");
        params.put("unexpected_param", "x".repeat(300));

        Response response = assumeNotBlocked(searchResults.get(SearchResultsClient.PATH, params, true));
        assertThat(response.getStatusCode()).as("HTTP status for malformed params").isLessThan(500);
    }

    @Test(groups = "security", description = "Destination text is never reflected back as executable markup")
    @Severity(SeverityLevel.CRITICAL)
    public void destinationIsNotReflectedAsMarkup() {
        String payload = "<script>alert('qa')</script>";
        Response response = assumeNotBlocked(searchResults.search(SearchQuery.of(payload, TestData.checkin(), 2, 2)));
        assertThat(response.getStatusCode()).isLessThan(500);
        assertThat(response.getBody().asString()).as("raw payload in HTML").doesNotContain(payload);
    }
}
