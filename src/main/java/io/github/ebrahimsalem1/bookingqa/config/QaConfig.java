package io.github.ebrahimsalem1.bookingqa.config;

import io.github.ebrahimsalem1.bookingqa.mock.MockBookingServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

/**
 * Single source of configuration.
 *
 * <p>Resolution order (highest wins): JVM system property &gt; environment variable &gt;
 * {@code config/<env>.properties} &gt; {@code config/default.properties}.
 * Environment variable names are derived from the key: {@code demand.token -> DEMAND_TOKEN}.
 *
 * <p>In {@link Environment#MOCK} the placeholder {@code {mock}} inside any URL is replaced by
 * the base URL of the in-process WireMock server, which is started on demand on a free port.
 */
public final class QaConfig {

    private static final String MOCK_PLACEHOLDER = "{mock}";
    private static volatile QaConfig instance;

    private final Environment environment;
    private final Properties properties = new Properties();

    private QaConfig(Environment environment) {
        this.environment = environment;
        load("config/default.properties", true);
        load("config/" + environment.key() + ".properties", false);
    }

    public static QaConfig get() {
        QaConfig local = instance;
        if (local == null) {
            synchronized (QaConfig.class) {
                local = instance;
                if (local == null) {
                    local = new QaConfig(Environment.from(System.getProperty("qa.env", System.getenv("QA_ENV"))));
                    instance = local;
                }
            }
        }
        return local;
    }

    /** Re-reads configuration; used by the agent and by unit tests that flip the environment. */
    public static synchronized void reload() {
        instance = null;
    }

    private void load(String resource, boolean required) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                if (required) {
                    throw new IllegalStateException("Missing required config resource: " + resource);
                }
                return;
            }
            properties.load(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read " + resource, e);
        }
    }

    // ------------------------------------------------------------------ raw access

    public Optional<String> find(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return Optional.of(sys.trim());
        }
        String env = System.getenv(key.toUpperCase(Locale.ROOT).replace('.', '_').replace('-', '_'));
        if (env != null && !env.isBlank()) {
            return Optional.of(env.trim());
        }
        String file = properties.getProperty(key);
        return file == null || file.isBlank() ? Optional.empty() : Optional.of(file.trim());
    }

    public String require(String key) {
        return find(key).orElseThrow(() -> new IllegalStateException("Missing config key: " + key));
    }

    public int intValue(String key) {
        return Integer.parseInt(require(key));
    }

    public boolean boolValue(String key) {
        return Boolean.parseBoolean(require(key));
    }

    private String url(String key) {
        String value = require(key);
        if (value.contains(MOCK_PLACEHOLDER)) {
            value = value.replace(MOCK_PLACEHOLDER, MockBookingServer.ensureStarted());
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    // ------------------------------------------------------------------ typed accessors

    public Environment environment() {
        return environment;
    }

    public boolean isMock() {
        return environment == Environment.MOCK;
    }

    /** booking.com web front door (search results page, security headers). */
    public String webBaseUrl() {
        return url("web.base.url");
    }

    /** The JSON endpoint the booking.com search box calls while the user types. */
    public String autocompleteBaseUrl() {
        return url("autocomplete.base.url");
    }

    /** Booking.com Demand API (official partner API) base URL incl. version, e.g. .../3.1 */
    public String demandBaseUrl() {
        return url("demand.base.url");
    }

    public Optional<String> demandToken() {
        return find("demand.token");
    }

    public Optional<String> demandAffiliateId() {
        return find("demand.affiliate.id");
    }

    public boolean hasDemandCredentials() {
        return demandToken().isPresent() && demandAffiliateId().isPresent();
    }

    public String language() {
        return require("booking.language");
    }

    public String currency() {
        return require("booking.currency");
    }

    public String bookerCountry() {
        return require("booking.booker.country");
    }

    public String userAgent() {
        return require("http.user.agent");
    }

    public int connectTimeoutMs() {
        return intValue("http.connect.timeout.ms");
    }

    public int readTimeoutMs() {
        return intValue("http.read.timeout.ms");
    }

    public long webSlaMs() {
        return intValue("sla.web.ms");
    }

    public long apiSlaMs() {
        return intValue("sla.api.ms");
    }

    /** What to do when booking.com bot protection challenges us: {@code skip} or {@code fail}. */
    public BotBlockPolicy botBlockPolicy() {
        return BotBlockPolicy.valueOf(require("guard.bot.block.policy").toUpperCase(Locale.ROOT));
    }

    public int maxTransientRetries() {
        return intValue("retry.transient.max");
    }

    public enum BotBlockPolicy { SKIP, FAIL }
}
