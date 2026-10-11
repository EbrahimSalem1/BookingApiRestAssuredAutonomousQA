package io.github.ebrahimsalem1.bookingqa.autonomous.triage;

import io.github.ebrahimsalem1.bookingqa.core.guard.EnvironmentGuard;
import org.testng.SkipException;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic, rule-based triage of a failed or skipped check. Rules are ordered from the most
 * specific signal to the least; the first match wins. Being rule-based (not LLM-based) keeps the
 * verdict reproducible and explainable; the optional LLM advisor only adds commentary on top.
 */
public final class FailureTriager {

    private static final Pattern STATUS = Pattern.compile("(?:status code|but was|HTTP)\\s*<?(\\d{3})>?", Pattern.CASE_INSENSITIVE);
    private static final List<String> NETWORK_EXCEPTIONS = List.of(
            "SocketTimeoutException", "ConnectException", "UnknownHostException", "ConnectTimeoutException",
            "NoHttpResponseException", "SSLHandshakeException", "SocketException", "HttpHostConnectException");
    private static final List<String> SCHEMA_MARKERS = List.of(
            "json schema", "schema validation", "object has missing required properties",
            "instance type", "does not match the schema", "error: object instance has properties which are not allowed",
            "unrecognized field", "mismatchedinputexception", "cannot deserialize");
    private static final List<String> TEST_BUG_EXCEPTIONS = List.of(
            "NullPointerException", "ClassCastException", "IndexOutOfBoundsException",
            "IllegalArgumentException", "IllegalStateException", "NumberFormatException", "NoSuchElementException");

    private FailureTriager() {
    }

    public static TriageResult classify(Throwable t) {
        if (t == null) {
            return new TriageResult(FailureCategory.UNCLASSIFIED, "No throwable recorded", null);
        }
        String message = fullMessage(t);
        String lower = message.toLowerCase(Locale.ROOT);
        Integer status = extractStatus(message);

        if (t instanceof SkipException || isSkipLike(t)) {
            if (message.contains(EnvironmentGuard.ENV_BLOCKED)) {
                return new TriageResult(FailureCategory.ENVIRONMENT_BLOCKED, firstLine(message), status);
            }
            if (message.contains(EnvironmentGuard.NO_CREDENTIALS)) {
                return new TriageResult(FailureCategory.PRECONDITION_NOT_MET, firstLine(message), status);
            }
            return new TriageResult(FailureCategory.PRECONDITION_NOT_MET, "Skipped: " + firstLine(message), status);
        }
        if (message.contains(EnvironmentGuard.ENV_BLOCKED)) {
            return new TriageResult(FailureCategory.ENVIRONMENT_BLOCKED, firstLine(message), status);
        }
        String networkCause = networkCause(t);
        if (networkCause != null) {
            return new TriageResult(FailureCategory.TRANSIENT_INFRA, networkCause + ": " + firstLine(message), status);
        }
        if (status != null && status == 429) {
            return new TriageResult(FailureCategory.RATE_LIMITED, "HTTP 429 from server", status);
        }
        if (status != null && (status == 502 || status == 503 || status == 504)) {
            return new TriageResult(FailureCategory.TRANSIENT_INFRA, "Gateway status " + status, status);
        }
        if (status != null && status >= 500) {
            return new TriageResult(FailureCategory.SERVER_ERROR, "Server error " + status + ": " + firstLine(message), status);
        }
        if (SCHEMA_MARKERS.stream().anyMatch(lower::contains)) {
            return new TriageResult(FailureCategory.CONTRACT_DRIFT, firstLine(message), status);
        }
        if (lower.contains("response time")) {
            return new TriageResult(FailureCategory.PERFORMANCE_DEGRADATION, firstLine(message), status);
        }
        if (t instanceof AssertionError) {
            return new TriageResult(FailureCategory.FUNCTIONAL_DEFECT, firstLine(message), status);
        }
        if (TEST_BUG_EXCEPTIONS.contains(t.getClass().getSimpleName())) {
            return new TriageResult(FailureCategory.TEST_DEFECT, t.getClass().getSimpleName() + ": " + firstLine(message), status);
        }
        return new TriageResult(FailureCategory.UNCLASSIFIED, t.getClass().getSimpleName() + ": " + firstLine(message), status);
    }

    private static boolean isSkipLike(Throwable t) {
        String n = t.getClass().getSimpleName();
        return n.equals("TestAbortedException") || n.equals("AssumptionViolatedException");
    }

    private static String networkCause(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (NETWORK_EXCEPTIONS.contains(c.getClass().getSimpleName())) {
                return c.getClass().getSimpleName();
            }
            if (c.getCause() == c) {
                break;
            }
        }
        return null;
    }

    static Integer extractStatus(String message) {
        Matcher m = STATUS.matcher(message);
        Integer last = null;
        while (m.find()) {
            int code = Integer.parseInt(m.group(1));
            if (code >= 100 && code <= 599) {
                last = code; // "Expected status code <200> but was <503>" -> the actual one is last
            }
        }
        return last;
    }

    private static String fullMessage(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null && sb.length() < 4_000; c = c.getCause()) {
            if (c.getMessage() != null) {
                sb.append(c.getMessage()).append('\n');
            }
            if (c.getCause() == c) {
                break;
            }
        }
        return sb.toString().trim();
    }

    private static String firstLine(String message) {
        String trimmed = message.strip();
        int nl = trimmed.indexOf('\n');
        String line = nl < 0 ? trimmed : trimmed.substring(0, nl);
        return line.length() > 300 ? line.substring(0, 300) + "..." : line;
    }

    public record TriageResult(FailureCategory category, String reason, Integer httpStatus) {
    }
}
