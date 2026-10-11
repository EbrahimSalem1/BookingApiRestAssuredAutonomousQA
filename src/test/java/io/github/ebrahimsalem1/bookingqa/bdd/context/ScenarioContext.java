package io.github.ebrahimsalem1.bookingqa.bdd.context;

import io.github.ebrahimsalem1.bookingqa.models.demand.DemandResponses;
import io.restassured.response.Response;

import java.time.LocalDate;

/**
 * State shared between step classes of one scenario. Injected by PicoContainer, so a fresh
 * instance per scenario: no static state, scenarios stay independent and parallel-safe.
 */
public class ScenarioContext {

    public LocalDate checkin;
    public LocalDate checkout;
    public int adults = 2;

    public Response lastResponse;

    public Long accommodationId;
    public DemandResponses.Product chosenProduct;
}
