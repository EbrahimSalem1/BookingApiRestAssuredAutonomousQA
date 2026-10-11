package io.github.ebrahimsalem1.bookingqa.autonomous.ledger;

import io.github.ebrahimsalem1.bookingqa.autonomous.triage.FailureCategory;

import java.util.List;

/**
 * One executed check.
 *
 * @param id        stable identifier, {@code Class#method[params]} or {@code Scenario: name}
 * @param category  triage category for non-passing outcomes, null when passed
 * @param attempt   1 for the first execution, 2+ for transient retries
 */
public record TestOutcome(String id,
                          String description,
                          List<String> groups,
                          Status status,
                          long durationMs,
                          FailureCategory category,
                          String reason,
                          Integer httpStatus,
                          int attempt,
                          String phase) {

    public enum Status { PASSED, FAILED, SKIPPED, RETRIED }

    public boolean blocking() {
        return status == Status.FAILED && category != null && category.blocking();
    }
}
