package io.github.ebrahimsalem1.bookingqa.clients.web;

import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import io.github.ebrahimsalem1.bookingqa.core.http.SpecFactory;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/** The server-rendered search results page and other HTML entry points of www.booking.com. */
public class SearchResultsClient {

    public static final String PATH = "/searchresults.html";

    public Response search(SearchQuery query) {
        return Allure.step("GET " + PATH + " " + query, () -> given()
                .spec(SpecFactory.web())
                .queryParams(query.toParams())
                .when()
                .get(PATH)
                .andReturn());
    }

    /** Free-form GET for robustness checks (unknown params, malformed values). */
    public Response get(String path, Map<String, ?> params, boolean followRedirects) {
        return Allure.step("GET " + path + " " + params, () -> given()
                .spec(SpecFactory.web(followRedirects))
                .queryParams(params)
                .when()
                .get(path)
                .andReturn());
    }

    /** Search criteria exactly as the booking.com search form submits them. */
    public record SearchQuery(String destination, LocalDate checkin, LocalDate checkout,
                              int adults, int rooms, int children) {

        public static SearchQuery of(String destination, LocalDate checkin, int nights, int adults) {
            return new SearchQuery(destination, checkin, checkin.plusDays(nights), adults, 1, 0);
        }

        public Map<String, Object> toParams() {
            QaConfig cfg = QaConfig.get();
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("ss", destination);
            if (checkin != null) {
                p.put("checkin", checkin.toString());
            }
            if (checkout != null) {
                p.put("checkout", checkout.toString());
            }
            p.put("group_adults", adults);
            p.put("no_rooms", rooms);
            p.put("group_children", children);
            p.put("lang", cfg.language());
            p.put("selected_currency", cfg.currency());
            return p;
        }

        @Override
        public String toString() {
            return "'" + destination + "' " + checkin + " -> " + checkout + ", " + adults + " adults";
        }
    }
}
