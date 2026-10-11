package io.github.ebrahimsalem1.bookingqa.autonomous.explore;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import io.github.ebrahimsalem1.bookingqa.core.guard.BotProtectionDetector;
import io.github.ebrahimsalem1.bookingqa.core.http.SpecFactory;
import io.github.ebrahimsalem1.bookingqa.data.TestData;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * The agent's eyes. Sends every probe of the catalogue once and records status, latency,
 * content type, edge-protection signals, security headers and the JSON shape, compared to a
 * recorded baseline. Never throws: an unreachable surface is an observation, not a crash.
 */
public class ApiExplorer {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExplorer.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final List<String> NOTABLE_HEADERS = List.of(
            "strict-transport-security", "content-security-policy", "x-content-type-options",
            "x-frame-options", "referrer-policy", "server", "x-amzn-waf-action", "x-cache", "location", "cache-control");

    public List<Probe> loadCatalogue() {
        try (InputStream in = resource("autonomous/probes.yaml")) {
            if (in == null) {
                throw new IllegalStateException("autonomous/probes.yaml not found on classpath");
            }
            String raw = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("${checkin}", TestData.checkin().toString())
                    .replace("${checkout}", TestData.checkout(3).toString())
                    .replace("${pastCheckin}", TestData.checkin().minusYears(2).toString());
            return YAML.readValue(raw, new TypeReference<List<Probe>>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<Observation> explore() {
        List<Observation> observations = new ArrayList<>();
        for (Probe probe : loadCatalogue()) {
            Observation o = observe(probe);
            observations.add(o);
            LOG.info("probe {} -> {} {} ({} ms){}", probe.name(), o.health(), o.status(), o.latencyMs(),
                    o.blockedReason() == null ? "" : " blocked: " + o.blockedReason());
        }
        return observations;
    }

    Observation observe(Probe probe) {
        String request = probe.method() + " " + probe.target() + probe.path() + (probe.query() == null ? "" : " " + probe.query());
        if ("demand".equals(probe.target()) && !QaConfig.get().hasDemandCredentials()) {
            return new Observation(probe.name(), probe.target(), probe.area(), probe.risk(), request,
                    null, null, null, null, null, Map.of(), List.of(), ShapeFingerprint.compare(null, List.of()),
                    null, Observation.Health.NOT_PROBED, "No Demand API credentials (DEMAND_TOKEN / DEMAND_AFFILIATE_ID)", null);
        }
        RequestSpecification spec = spec(probe);
        long started = System.nanoTime();
        try {
            RequestSpecification call = given().spec(spec);
            if (probe.query() != null) {
                call.queryParams(probe.query());
            }
            if (probe.body() != null && !probe.body().isNull()) {
                call.body(JSON.writeValueAsString(probe.body()));
            }
            Response r = call.request(probe.method().toUpperCase(Locale.ROOT), probe.path()).andReturn();
            long latency = r.getTime() > 0 ? r.getTime() : (System.nanoTime() - started) / 1_000_000;
            return analyse(probe, request, r, latency);
        } catch (Exception e) {
            return new Observation(probe.name(), probe.target(), probe.area(), probe.risk(), request,
                    null, (System.nanoTime() - started) / 1_000_000, null, null, null, Map.of(), List.of(),
                    ShapeFingerprint.compare(null, List.of()), e.getClass().getSimpleName() + ": " + e.getMessage(),
                    Observation.Health.UNREACHABLE, "Surface not reachable from this network", null);
        }
    }

    private Observation analyse(Probe probe, String request, Response r, long latency) {
        String body = r.getBody().asString();
        String blocked = BotProtectionDetector.detect(r).orElse(null);

        Map<String, String> headers = new LinkedHashMap<>();
        for (String h : NOTABLE_HEADERS) {
            String v = r.getHeader(h);
            if (v != null) {
                headers.put(h, v.length() > 160 ? v.substring(0, 160) + "..." : v);
            }
        }

        List<String> shape = List.of();
        String excerpt = null;
        if (probe.json() && blocked == null) {
            if (body.isBlank()) {
                shape = List.of("$:empty");
            } else {
                try {
                    shape = ShapeFingerprint.of(JSON.readTree(body));
                    excerpt = body.length() > 600 ? body.substring(0, 600) + "..." : body;
                } catch (IOException e) {
                    shape = List.of("$:not-json");
                }
            }
        }
        Observation.ShapeDrift drift = ShapeFingerprint.compare(loadBaseline(probe.name()), shape);

        Observation.Health health;
        String note;
        boolean statusOk = probe.expectStatus() == null || probe.expectStatus().isEmpty()
                || probe.expectStatus().contains(r.getStatusCode());
        long sla = "web".equals(probe.target()) ? QaConfig.get().webSlaMs() : QaConfig.get().apiSlaMs();
        if (blocked != null) {
            health = Observation.Health.BLOCKED;
            note = "Edge protection challenged the probe; checks on this surface are inconclusive from this network";
        } else if (!statusOk) {
            health = Observation.Health.UNEXPECTED_STATUS;
            note = "Expected " + probe.expectStatus() + " but got " + r.getStatusCode();
        } else if (drift.breaking()) {
            health = Observation.Health.CONTRACT_DRIFT;
            note = "Fields disappeared vs baseline: " + drift.removed();
        } else if (latency > sla) {
            health = Observation.Health.SLOW;
            note = "Latency " + latency + " ms above SLO " + sla + " ms";
        } else {
            health = Observation.Health.HEALTHY;
            if (!probe.json()) {
                note = "HTML surface (status, headers and latency tracked; no JSON shape)";
            } else if (drift.drifted()) {
                note = "Additive change vs baseline: " + drift.added();
            } else {
                note = drift.baselineFound() ? "Matches baseline" : "No baseline yet";
            }
        }

        return new Observation(probe.name(), probe.target(), probe.area(), probe.risk(), request,
                r.getStatusCode(), latency, r.getContentType(), body.length(), blocked, headers, shape, drift,
                null, health, note, excerpt);
    }

    private static RequestSpecification spec(Probe probe) {
        return switch (probe.target()) {
            case "web" -> SpecFactory.web(probe.followRedirects());
            case "autocomplete" -> SpecFactory.autocomplete();
            case "demand" -> SpecFactory.demand();
            case "demand-anonymous" -> SpecFactory.demandAnonymous();
            default -> throw new IllegalArgumentException("Unknown probe target: " + probe.target());
        };
    }

    static List<String> loadBaseline(String probeName) {
        String env = QaConfig.get().environment().key();
        for (String path : List.of("autonomous/baselines/" + env + "/" + probeName + ".json", "autonomous/baselines/" + probeName + ".json")) {
            try (InputStream in = resource(path)) {
                if (in != null) {
                    return JSON.readValue(in, new TypeReference<List<String>>() {
                    });
                }
            } catch (IOException e) {
                LOG.warn("Unreadable baseline {}: {}", path, e.getMessage());
            }
        }
        return null;
    }

    private static InputStream resource(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    }
}
