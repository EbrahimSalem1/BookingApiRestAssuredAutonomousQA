package io.github.ebrahimsalem1.bookingqa.clients.web;

import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import io.github.ebrahimsalem1.bookingqa.core.http.SpecFactory;
import io.github.ebrahimsalem1.bookingqa.models.web.Autocomplete;
import io.qameta.allure.Allure;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

/** Destination suggestions behind the booking.com search box. */
public class AutocompleteClient {

    public static final String PATH = "/autocomplete.json";

    public Response suggest(String query) {
        return suggest(query, QaConfig.get().language(), 5);
    }

    public Response suggest(String query, String language, int size) {
        return send(new Autocomplete.Request(query, language, size),
                "Autocomplete '" + query + "' (" + language + ", size " + size + ")");
    }

    /** Sends an arbitrary body: used by robustness / fuzz checks. */
    public Response suggestRaw(Object body) {
        return send(body, "Autocomplete raw body");
    }

    private Response send(Object body, String stepName) {
        return Allure.step(stepName, () -> given()
                .spec(SpecFactory.autocomplete())
                .body(body)
                .when()
                .post(PATH)
                .andReturn());
    }

    public static Autocomplete.Response parse(Response response) {
        return response.as(Autocomplete.Response.class);
    }
}
