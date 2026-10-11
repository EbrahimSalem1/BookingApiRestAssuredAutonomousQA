package io.github.ebrahimsalem1.bookingqa.config;

import java.util.Locale;

/**
 * Where the checks are pointed.
 *
 * <ul>
 *   <li>{@link #LIVE}: public booking.com web endpoints and the production Demand API host
 *       (positive Demand API checks need partner credentials and are skipped without them).</li>
 *   <li>{@link #SANDBOX}: booking.com web endpoints and the Demand API <b>sandbox</b> host.</li>
 *   <li>{@link #MOCK}: everything served by an in-process WireMock modelled on the documented
 *       contracts. Hermetic, deterministic, the pull-request quality gate.</li>
 * </ul>
 */
public enum Environment {
    LIVE, SANDBOX, MOCK;

    public static Environment from(String raw) {
        if (raw == null || raw.isBlank()) {
            return LIVE;
        }
        return Environment.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
