package io.github.ebrahimsalem1.bookingqa.tests.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ebrahimsalem1.bookingqa.autonomous.explore.Observation;
import io.github.ebrahimsalem1.bookingqa.autonomous.explore.ShapeFingerprint;
import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.TestOutcome;
import io.github.ebrahimsalem1.bookingqa.autonomous.plan.ExecutionPlan;
import io.github.ebrahimsalem1.bookingqa.autonomous.plan.RiskBasedPlanner;
import io.github.ebrahimsalem1.bookingqa.autonomous.report.Verdict;
import io.github.ebrahimsalem1.bookingqa.autonomous.triage.FailureCategory;
import io.github.ebrahimsalem1.bookingqa.autonomous.triage.FailureTriager;
import io.github.ebrahimsalem1.bookingqa.core.guard.BotProtectionDetector;
import io.github.ebrahimsalem1.bookingqa.core.guard.EnvironmentGuard;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the test system. The autonomous decisions (triage, bot detection, drift, planning,
 * verdict) are pure functions, so they are verified offline and run first in every pipeline.
 */
@Epic("Autonomous QA engine")
@Feature("Self-tests")
@Test(groups = "unit")
public class FrameworkSelfTest {

    // ------------------------------------------------------------------ triage

    @DataProvider(name = "triage")
    public Object[][] triage() {
        return new Object[][]{
                {new AssertionError("1 expectation failed.\nExpected status code <200> but was <503>."), FailureCategory.TRANSIENT_INFRA},
                {new AssertionError("Expected status code <200> but was <500>."), FailureCategory.SERVER_ERROR},
                {new AssertionError("Expected status code <200> but was <429>."), FailureCategory.RATE_LIMITED},
                {new AssertionError("Response body doesn't match expectation.\nExpected: The content to match the given JSON schema.\nerror: object has missing required properties ([\"label\"])"), FailureCategory.CONTRACT_DRIFT},
                {new AssertionError("Expected response time was not a value less than <3000L> milliseconds, was 4120 milliseconds"), FailureCategory.PERFORMANCE_DEGRADATION},
                {new AssertionError("[top suggestion label] Expecting actual: \"Giza\" to contain \"Cairo\""), FailureCategory.FUNCTIONAL_DEFECT},
                {new RuntimeException("I/O error", new SocketTimeoutException("Read timed out")), FailureCategory.TRANSIENT_INFRA},
                {new SkipException(EnvironmentGuard.ENV_BLOCKED + " challenged"), FailureCategory.ENVIRONMENT_BLOCKED},
                {new SkipException(EnvironmentGuard.NO_CREDENTIALS + " set DEMAND_TOKEN"), FailureCategory.PRECONDITION_NOT_MET},
                {new NullPointerException("Cannot invoke \"String.length()\""), FailureCategory.TEST_DEFECT},
        };
    }

    @Test(dataProvider = "triage", description = "Failures are classified into the right category")
    public void triageClassifiesFailures(Throwable failure, FailureCategory expected) {
        assertThat(FailureTriager.classify(failure).category()).isEqualTo(expected);
    }

    @Test(description = "Only transient categories are retried")
    public void onlyTransientCategoriesAreRetryable() {
        assertThat(FailureCategory.FUNCTIONAL_DEFECT.retryable()).isFalse();
        assertThat(FailureCategory.CONTRACT_DRIFT.retryable()).isFalse();
        assertThat(FailureCategory.TRANSIENT_INFRA.retryable()).isTrue();
        assertThat(FailureCategory.RATE_LIMITED.retryable()).isTrue();
    }

    // ------------------------------------------------------------------ bot protection

    @Test(description = "AWS WAF challenge header is recognised")
    public void wafHeaderIsDetected() {
        assertThat(BotProtectionDetector.detect(202, "challenge", null, "")).isPresent();
    }

    @Test(description = "Challenge page body is recognised")
    public void challengePageIsDetected() {
        String body = "<html><script src=\"https://x.token.awswaf.com/challenge.js\"></script></html>";
        assertThat(BotProtectionDetector.detect(202, null, null, body)).isPresent();
    }

    @Test(description = "A normal page and a JSON API 403 are not mistaken for a challenge")
    public void applicationResponsesAreNotFlagged() {
        assertThat(BotProtectionDetector.detect(200, null, null, "<html>Cairo hotels</html>")).isEmpty();
        assertThat(BotProtectionDetector.detect(403, null, null, "{\"errors\":[{\"id\":\"access_denied\"}]}")).isEmpty();
    }

    // ------------------------------------------------------------------ contract drift

    @Test(description = "Removed fields are breaking drift, added fields are not")
    public void shapeDriftDistinguishesBreakingFromAdditive() throws Exception {
        ObjectMapper m = new ObjectMapper();
        List<String> baseline = ShapeFingerprint.of(m.readTree("{\"results\":[{\"dest_id\":\"1\",\"label\":\"Cairo\"}]}"));
        List<String> added = ShapeFingerprint.of(m.readTree("{\"results\":[{\"dest_id\":\"1\",\"label\":\"Cairo\",\"new\":1}]}"));
        List<String> removed = ShapeFingerprint.of(m.readTree("{\"results\":[{\"dest_id\":\"1\"}]}"));

        assertThat(ShapeFingerprint.compare(baseline, added).breaking()).isFalse();
        assertThat(ShapeFingerprint.compare(baseline, added).drifted()).isTrue();
        assertThat(ShapeFingerprint.compare(baseline, removed).breaking()).isTrue();
        assertThat(ShapeFingerprint.compare(baseline, removed).removed()).containsExactly("$.results[].label:string");
    }

    // ------------------------------------------------------------------ planning

    @Test(description = "Fully challenged surfaces are excluded with a reason; healthy ones run")
    public void plannerExcludesBlockedSurfaces() {
        List<Observation> obs = List.of(
                observation("search-results-page", "web", Observation.Health.BLOCKED, "AWS WAF"),
                observation("autocomplete-city", "autocomplete", Observation.Health.HEALTHY, null),
                observation("demand-search", "demand", Observation.Health.NOT_PROBED, null));

        ExecutionPlan plan = new RiskBasedPlanner().plan(obs);

        assertThat(plan.excludedGroups()).containsKeys("web", "demand-partner").doesNotContainKey("autocomplete");
        assertThat(plan.includedGroups()).contains("autocomplete", "smoke");
        assertThat(plan.cucumberTags()).contains("not @web").contains("not @demand-partner");
        assertThat(plan.surfaceHealth()).containsEntry("web", ExecutionPlan.SurfaceHealth.BLOCKED);
    }

    // ------------------------------------------------------------------ verdict

    @Test(description = "Blocking failure => FAIL; mostly skipped => INCONCLUSIVE; otherwise PASS")
    public void verdictRules() {
        TestOutcome pass = outcome(TestOutcome.Status.PASSED, null);
        TestOutcome skip = outcome(TestOutcome.Status.SKIPPED, FailureCategory.ENVIRONMENT_BLOCKED);
        TestOutcome defect = outcome(TestOutcome.Status.FAILED, FailureCategory.FUNCTIONAL_DEFECT);
        TestOutcome slow = outcome(TestOutcome.Status.FAILED, FailureCategory.PERFORMANCE_DEGRADATION);

        assertThat(Verdict.of(List.of(pass, pass, slow))).isEqualTo(Verdict.PASS);
        assertThat(Verdict.of(List.of(pass, defect))).isEqualTo(Verdict.FAIL);
        assertThat(Verdict.of(List.of(pass, skip, skip, skip))).isEqualTo(Verdict.INCONCLUSIVE);
    }

    private static Observation observation(String probe, String target, Observation.Health health, String blocked) {
        return new Observation(probe, target, "search", 5, "GET /", health == Observation.Health.NOT_PROBED ? null : 200, 100L,
                "text/html", 10, blocked, Map.of(), List.of(), ShapeFingerprint.compare(null, List.of()), null, health, "note", null);
    }

    private static TestOutcome outcome(TestOutcome.Status status, FailureCategory category) {
        return new TestOutcome("X#y", "", List.of("search"), status, 10, category, "r", null, 1, "unit");
    }
}
