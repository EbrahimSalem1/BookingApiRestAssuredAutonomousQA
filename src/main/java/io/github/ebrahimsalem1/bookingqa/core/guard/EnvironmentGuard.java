package io.github.ebrahimsalem1.bookingqa.core.guard;

import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import io.restassured.response.Response;
import org.testng.SkipException;

import java.util.Optional;

/**
 * Pre-conditions that, when unmet, make a check <i>inconclusive</i> rather than failed.
 * Skips always carry a machine-readable tag in square brackets; the triage engine keys on it.
 */
public final class EnvironmentGuard {

    public static final String ENV_BLOCKED = "[ENV_BLOCKED]";
    public static final String NO_CREDENTIALS = "[NO_CREDENTIALS]";

    private EnvironmentGuard() {
    }

    /**
     * Call right after a live request. If booking.com's edge challenged us, skip (default) or
     * fail (when {@code guard.bot.block.policy=fail}, e.g. from an allow-listed network).
     */
    public static Response assumeNotBlocked(Response response) {
        Optional<String> block = BotProtectionDetector.detect(response);
        if (block.isPresent()) {
            String msg = ENV_BLOCKED + " booking.com edge protection answered instead of the application: " + block.get();
            if (QaConfig.get().botBlockPolicy() == QaConfig.BotBlockPolicy.SKIP) {
                throw new SkipException(msg);
            }
            throw new AssertionError(msg);
        }
        return response;
    }

    /** Positive Demand API checks need partner credentials (always present in mock). */
    public static void requireDemandCredentials() {
        if (!QaConfig.get().hasDemandCredentials()) {
            throw new SkipException(NO_CREDENTIALS + " set DEMAND_TOKEN and DEMAND_AFFILIATE_ID "
                    + "(Booking.com Demand API partner credentials) or run with -Dqa.env=mock");
        }
    }
}
