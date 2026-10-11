package io.github.ebrahimsalem1.bookingqa.autonomous.triage;

/**
 * What a non-passing check most likely means. {@link #blocking} categories make the run verdict
 * FAIL; {@link #retryable} ones are retried before they are reported.
 */
public enum FailureCategory {

    FUNCTIONAL_DEFECT(true, false, "Behaviour differs from the expected business rule"),
    CONTRACT_DRIFT(true, false, "Response shape no longer matches the published/recorded contract"),
    SERVER_ERROR(true, false, "Server answered 5xx to a valid or invalid request (robustness bug)"),
    TEST_DEFECT(true, false, "Bug in the automation code itself (NPE, bad cast, bad data)"),
    PERFORMANCE_DEGRADATION(false, false, "Response slower than the service level objective"),
    TRANSIENT_INFRA(false, true, "Network / gateway hiccup (timeouts, resets, 502/503/504)"),
    RATE_LIMITED(false, true, "Throttled (HTTP 429)"),
    ENVIRONMENT_BLOCKED(false, false, "booking.com edge bot protection answered instead of the app"),
    PRECONDITION_NOT_MET(false, false, "Missing credentials or prerequisites; check could not run"),
    UNCLASSIFIED(true, false, "Needs a human look");

    private final boolean blocking;
    private final boolean retryable;
    private final String meaning;

    FailureCategory(boolean blocking, boolean retryable, String meaning) {
        this.blocking = blocking;
        this.retryable = retryable;
        this.meaning = meaning;
    }

    public boolean blocking() {
        return blocking;
    }

    public boolean retryable() {
        return retryable;
    }

    public String meaning() {
        return meaning;
    }
}
