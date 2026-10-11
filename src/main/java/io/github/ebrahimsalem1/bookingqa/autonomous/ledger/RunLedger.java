package io.github.ebrahimsalem1.bookingqa.autonomous.ledger;

import io.github.ebrahimsalem1.bookingqa.autonomous.triage.FailureCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory record of every check outcome of the current JVM run. Written by the TestNG
 * listener, read by the agent and the reporters. Thread-safe for parallel runs.
 */
public final class RunLedger {

    private static final List<TestOutcome> OUTCOMES = Collections.synchronizedList(new ArrayList<>());

    private RunLedger() {
    }

    public static void record(TestOutcome outcome) {
        OUTCOMES.add(outcome);
    }

    public static List<TestOutcome> snapshot() {
        synchronized (OUTCOMES) {
            return List.copyOf(OUTCOMES);
        }
    }

    public static void clear() {
        OUTCOMES.clear();
    }

    public static Map<TestOutcome.Status, Long> countByStatus(List<TestOutcome> outcomes) {
        return outcomes.stream().collect(Collectors.groupingBy(TestOutcome::status, Collectors.counting()));
    }

    public static Map<FailureCategory, Long> countByCategory(List<TestOutcome> outcomes) {
        return outcomes.stream()
                .filter(o -> o.category() != null)
                .collect(Collectors.groupingBy(TestOutcome::category, Collectors.counting()));
    }
}
