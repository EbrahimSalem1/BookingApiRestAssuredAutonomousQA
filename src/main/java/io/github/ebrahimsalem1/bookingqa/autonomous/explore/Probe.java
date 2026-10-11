package io.github.ebrahimsalem1.bookingqa.autonomous.explore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * One exploratory probe from {@code autonomous/probes.yaml}: a cheap request the agent sends
 * before planning, to learn how each booking.com surface behaves <i>right now</i>.
 *
 * @param target  web | autocomplete | demand | demand-anonymous
 * @param area    product area the probe informs (search, checkout, platform)
 * @param risk    1 (cosmetic) .. 5 (revenue critical); drives execution order
 * @param json    true when the response is expected to be JSON (enables shape fingerprinting)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Probe(String name,
                    String description,
                    String target,
                    String area,
                    int risk,
                    String method,
                    String path,
                    Map<String, String> query,
                    JsonNode body,
                    List<Integer> expectStatus,
                    boolean json,
                    boolean followRedirects) {
}
