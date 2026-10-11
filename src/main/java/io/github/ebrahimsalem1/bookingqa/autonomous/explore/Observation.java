package io.github.ebrahimsalem1.bookingqa.autonomous.explore;

import java.util.List;
import java.util.Map;

/**
 * What the explorer saw when it sent one {@link Probe}.
 * {@code excerpt} keeps the first characters of a JSON body as evidence for exploratory notes.
 */
public record Observation(String probe,
                          String target,
                          String area,
                          int risk,
                          String request,
                          Integer status,
                          Long latencyMs,
                          String contentType,
                          Integer bytes,
                          String blockedReason,
                          Map<String, String> notableHeaders,
                          List<String> shape,
                          ShapeDrift drift,
                          String error,
                          Health health,
                          String note,
                          String excerpt) {

    public enum Health { HEALTHY, SLOW, UNEXPECTED_STATUS, CONTRACT_DRIFT, BLOCKED, UNREACHABLE, NOT_PROBED }

    public record ShapeDrift(boolean baselineFound, List<String> added, List<String> removed) {
        public boolean drifted() {
            return baselineFound && (!removed.isEmpty() || !added.isEmpty());
        }

        /** Removed fields break consumers; added fields usually do not. */
        public boolean breaking() {
            return baselineFound && !removed.isEmpty();
        }
    }
}
