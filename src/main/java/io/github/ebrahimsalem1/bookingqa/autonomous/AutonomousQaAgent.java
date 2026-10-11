package io.github.ebrahimsalem1.bookingqa.autonomous;

import io.github.ebrahimsalem1.bookingqa.autonomous.execute.TestNgExecutor;
import io.github.ebrahimsalem1.bookingqa.autonomous.explore.ApiExplorer;
import io.github.ebrahimsalem1.bookingqa.autonomous.explore.Observation;
import io.github.ebrahimsalem1.bookingqa.autonomous.heal.ContractDriftHealer;
import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.RunLedger;
import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.TestOutcome;
import io.github.ebrahimsalem1.bookingqa.autonomous.llm.LlmAdvisor;
import io.github.ebrahimsalem1.bookingqa.autonomous.plan.ExecutionPlan;
import io.github.ebrahimsalem1.bookingqa.autonomous.plan.RiskBasedPlanner;
import io.github.ebrahimsalem1.bookingqa.autonomous.report.AgentReport;
import io.github.ebrahimsalem1.bookingqa.autonomous.report.LedgerWriter;
import io.github.ebrahimsalem1.bookingqa.autonomous.report.Verdict;
import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import io.github.ebrahimsalem1.bookingqa.mock.MockBookingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The autonomous QA loop:
 *
 * <pre>
 *  OBSERVE  -> probe every booking.com surface (reachability, bot protection, latency, JSON shape)
 *  PLAN     -> decide what is worth running and in which order, with a written reason per decision
 *  EXECUTE  -> phase 1 smoke (circuit breaker), phase 2 full regression; transient failures retried
 *  TRIAGE   -> classify every non-pass (product defect / contract drift / 5xx / env-blocked / ...)
 *  HEAL     -> propose updated contract baselines for drift, never auto-accept breaking changes
 *  REPORT   -> Markdown + JSON + GitHub summary; optional LLM second opinion; exit code = verdict
 * </pre>
 *
 * Run: {@code mvn -Dqa.env=live test-compile exec:java}
 */
public final class AutonomousQaAgent {

    private static final Logger LOG = LoggerFactory.getLogger(AutonomousQaAgent.class);
    private static final String SUITE = "suites/regression.xml";

    private AutonomousQaAgent() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("qa.agent.active", "true");
        QaConfig.reload();
        QaConfig cfg = QaConfig.get();
        String started = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
        Path outDir = Path.of(cfg.require("agent.output.dir"));
        RunLedger.clear();
        LOG.info("Autonomous QA agent starting against '{}'", cfg.environment().key());

        // 1. OBSERVE
        List<Observation> observations = new ApiExplorer().explore();

        // 2. PLAN
        ExecutionPlan plan = new RiskBasedPlanner().plan(observations);
        LOG.info("Plan: health={} excluded={}", plan.surfaceHealth(), plan.excludedGroups().keySet());

        // 3. EXECUTE
        TestNgExecutor executor = new TestNgExecutor(SUITE);
        List<String> phaseNotes = new ArrayList<>();
        Set<String> excluded = new LinkedHashSet<>(plan.excludedGroups().keySet());

        List<TestOutcome> smoke = executor.run("smoke", List.of("smoke"), excluded, plan.cucumberTags());
        long smokePassed = count(smoke, TestOutcome.Status.PASSED);
        long smokeFailed = count(smoke, TestOutcome.Status.FAILED);
        double rate = smokePassed + smokeFailed == 0 ? 1.0 : (double) smokePassed / (smokePassed + smokeFailed);
        double minRate = Double.parseDouble(cfg.require("agent.smoke.min.pass.rate"));
        phaseNotes.add(String.format("Phase `smoke`: %d passed, %d failed, %d skipped (pass rate %.0f%%, threshold %.0f%%)",
                smokePassed, smokeFailed, count(smoke, TestOutcome.Status.SKIPPED), rate * 100, minRate * 100));

        if (rate < minRate) {
            phaseNotes.add("Circuit breaker OPEN: smoke pass rate below threshold, full regression not started "
                    + "(running it would only add noise on top of a broken baseline).");
        } else {
            Set<String> regressionExcludes = new LinkedHashSet<>(excluded);
            regressionExcludes.add("smoke");
            List<TestOutcome> regression = executor.run("regression", List.of(), regressionExcludes, plan.cucumberTags());
            phaseNotes.add(String.format("Phase `regression`: %d passed, %d failed, %d skipped, %d transient retries",
                    count(regression, TestOutcome.Status.PASSED), count(regression, TestOutcome.Status.FAILED),
                    count(regression, TestOutcome.Status.SKIPPED), count(regression, TestOutcome.Status.RETRIED)));
        }

        // 4. TRIAGE + verdict (deterministic)
        List<TestOutcome> outcomes = RunLedger.snapshot();
        Verdict verdict = Verdict.of(outcomes);
        String reason = verdictReason(verdict, outcomes, plan);
        if (verdict == Verdict.PASS && plan.surfaceHealth().values().stream()
                .anyMatch(h -> h == ExecutionPlan.SurfaceHealth.BLOCKED || h == ExecutionPlan.SurfaceHealth.UNREACHABLE)) {
            verdict = Verdict.INCONCLUSIVE;
            reason = "Everything that ran passed, but at least one surface could not be exercised from this network: "
                    + plan.excludedGroups().keySet();
        }

        // 5. HEAL
        var healing = new ContractDriftHealer().propose(observations, outDir);

        // 6. REPORT (+ optional AI second opinion)
        AgentReport draft = new AgentReport(cfg.environment().key(), started, now(), verdict, reason,
                observations, plan, phaseNotes, outcomes, healing, null);
        String advice = new LlmAdvisor().advise(draft.digest()).orElse(null);
        AgentReport report = new AgentReport(draft.environment(), started, now(), verdict, reason,
                observations, plan, phaseNotes, outcomes, healing, advice);
        report.write(outDir);
        LedgerWriter.publishSummary(report.toMarkdown());
        MockBookingServer.stop();

        LOG.info("Verdict {}: {} (report: {})", verdict, reason, outDir.resolve("report.md").toAbsolutePath());
        if (verdict == Verdict.FAIL) {
            throw new IllegalStateException("Autonomous QA verdict FAIL: " + reason);
        }
    }

    private static String verdictReason(Verdict verdict, List<TestOutcome> outcomes, ExecutionPlan plan) {
        long blocking = outcomes.stream().filter(TestOutcome::blocking).count();
        return switch (verdict) {
            case FAIL -> blocking + " blocking failure(s): " + outcomes.stream().filter(TestOutcome::blocking)
                    .map(o -> o.category() + " " + o.id()).limit(5).toList();
            case INCONCLUSIVE -> "Too few checks could exercise the product (most were skipped); see triage and excluded surfaces "
                    + plan.excludedGroups().keySet();
            case PASS -> "All executed checks passed";
        };
    }

    private static long count(List<TestOutcome> outcomes, TestOutcome.Status status) {
        return outcomes.stream().filter(o -> o.status() == status).count();
    }

    private static String now() {
        return OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }
}
