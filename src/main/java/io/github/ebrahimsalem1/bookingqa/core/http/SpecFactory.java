package io.github.ebrahimsalem1.bookingqa.core.http;

import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

/**
 * One place that knows how to talk to each booking.com surface. Clients ask for a spec,
 * tests never build headers or base URIs themselves.
 */
public final class SpecFactory {

    private SpecFactory() {
    }

    private static RestAssuredConfig httpConfig(boolean followRedirects) {
        QaConfig cfg = QaConfig.get();
        return RestAssuredConfig.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                        .setParam("http.connection.timeout", cfg.connectTimeoutMs())
                        .setParam("http.socket.timeout", cfg.readTimeoutMs()))
                .redirect(RedirectConfig.redirectConfig().followRedirects(followRedirects));
    }

    private static RequestSpecBuilder browserLike(String baseUri, boolean followRedirects) {
        QaConfig cfg = QaConfig.get();
        return new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .setConfig(httpConfig(followRedirects))
                .addHeader("User-Agent", cfg.userAgent())
                .addHeader("Accept-Language", cfg.language() + ",en;q=0.8")
                .addFilter(new RedactingAllureFilter());
    }

    /** HTML pages on www.booking.com (search results page, front door). */
    public static RequestSpecification web() {
        return web(true);
    }

    public static RequestSpecification web(boolean followRedirects) {
        return browserLike(QaConfig.get().webBaseUrl(), followRedirects)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build();
    }

    /** The JSON autocomplete endpoint the search box calls (same call a browser makes). */
    public static RequestSpecification autocomplete() {
        QaConfig cfg = QaConfig.get();
        return browserLike(cfg.autocompleteBaseUrl(), true)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("Origin", "https://www.booking.com")
                .addHeader("Referer", "https://www.booking.com/")
                .build();
    }

    /** Booking.com Demand API without any credentials (for auth / negative checks). */
    public static RequestSpecification demandAnonymous() {
        return new RequestSpecBuilder()
                .setBaseUri(QaConfig.get().demandBaseUrl())
                .setConfig(httpConfig(false))
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addFilter(new RedactingAllureFilter())
                .build();
    }

    /** Booking.com Demand API with the given credentials. */
    public static RequestSpecification demand(String token, String affiliateId) {
        RequestSpecBuilder b = new RequestSpecBuilder().addRequestSpecification(demandAnonymous());
        if (token != null) {
            b.addHeader("Authorization", "Bearer " + token);
        }
        if (affiliateId != null) {
            b.addHeader("X-Affiliate-Id", affiliateId);
        }
        return b.build();
    }

    /** Booking.com Demand API with the configured credentials (env vars / CI secrets / mock). */
    public static RequestSpecification demand() {
        QaConfig cfg = QaConfig.get();
        return demand(cfg.demandToken().orElse(null), cfg.demandAffiliateId().orElse(null));
    }
}
