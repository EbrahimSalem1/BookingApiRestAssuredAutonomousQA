package io.github.ebrahimsalem1.bookingqa.autonomous.report;

import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.TestOutcome;

import java.util.List;

/**
 * Run verdict.
 * <ul>
 *   <li>PASS: nothing blocking failed and enough checks actually exercised the product.</li>
 *   <li>INCONCLUSIVE: nothing blocking failed, but most checks were skipped (blocked network,
 *       missing credentials). Explicitly not a green: absence of evidence is not evidence.</li>
 *   <li>FAIL: at least one blocking failure (functional, contract, 5xx, test defect).</li>
 * </ul>
 */
public enum Verdict {
    PASS, INCONCLUSIVE, FAIL;

    public static Verdict of(List<TestOutcome> outcomes) {
        List<TestOutcome> finals = outcomes.stream().filter(o -> o.status() != TestOutcome.Status.RETRIED).toList();
        if (finals.stream().anyMatch(TestOutcome::blocking)) {
            return FAIL;
        }
        long executed = finals.stream().filter(o -> o.status() != TestOutcome.Status.SKIPPED).count();
        long productChecks = finals.stream().filter(o -> !o.groups().contains("unit")).count();
        long productExecuted = finals.stream()
                .filter(o -> !o.groups().contains("unit"))
                .filter(o -> o.status() != TestOutcome.Status.SKIPPED)
                .count();
        if (executed == 0 || (productChecks > 0 && productExecuted * 2 < productChecks)) {
            return INCONCLUSIVE;
        }
        return PASS;
    }
}
