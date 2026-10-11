package io.github.ebrahimsalem1.bookingqa.autonomous.execute;

import io.cucumber.testng.PickleWrapper;
import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.RunLedger;
import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.TestOutcome;
import io.github.ebrahimsalem1.bookingqa.autonomous.report.LedgerWriter;
import io.github.ebrahimsalem1.bookingqa.autonomous.triage.FailureCategory;
import io.github.ebrahimsalem1.bookingqa.autonomous.triage.FailureTriager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Feeds every outcome (with its triage) into the {@link RunLedger} and, when a suite finishes,
 * writes the ledger and a triage summary to {@code target/autonomous}. Active for plain
 * {@code mvn test} runs as well as for the agent, so every run is triaged.
 */
public class AutonomousTestListener implements ITestListener, ISuiteListener {

    private static final Logger LOG = LoggerFactory.getLogger(AutonomousTestListener.class);

    @Override
    public void onTestSuccess(ITestResult result) {
        record(result, TestOutcome.Status.PASSED);
    }

    @Override
    public void onTestFailure(ITestResult result) {
        record(result, TestOutcome.Status.FAILED);
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        record(result, result.wasRetried() ? TestOutcome.Status.RETRIED : TestOutcome.Status.SKIPPED);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        record(result, TestOutcome.Status.FAILED);
    }

    @Override
    public void onFinish(ISuite suite) {
        if (Boolean.getBoolean("qa.agent.active")) {
            return; // the agent writes a consolidated report after all phases
        }
        LedgerWriter.write(RunLedger.snapshot());
    }

    private void record(ITestResult result, TestOutcome.Status status) {
        FailureTriager.TriageResult triage = null;
        if (status != TestOutcome.Status.PASSED) {
            triage = result.getThrowable() == null && status != TestOutcome.Status.FAILED
                    ? new FailureTriager.TriageResult(FailureCategory.PRECONDITION_NOT_MET,
                    "Skipped by a configuration method or dependency", null)
                    : FailureTriager.classify(result.getThrowable());
        }
        List<String> groups = Arrays.asList(result.getMethod().getGroups());
        TestOutcome outcome = new TestOutcome(
                id(result),
                result.getMethod().getDescription(),
                groups,
                status,
                result.getEndMillis() - result.getStartMillis(),
                triage == null ? null : triage.category(),
                triage == null ? null : triage.reason(),
                triage == null ? null : triage.httpStatus(),
                result.getMethod().getCurrentInvocationCount(),
                System.getProperty("qa.agent.phase", "maven"));
        RunLedger.record(outcome);
        if (status != TestOutcome.Status.PASSED) {
            LOG.info("{} {} -> {} : {}", status, outcome.id(), outcome.category(), outcome.reason());
        }
    }

    private static String id(ITestResult result) {
        Object[] params = result.getParameters();
        if (params != null && params.length > 0 && params[0] instanceof PickleWrapper pickle) {
            return "Scenario: " + pickle.getPickle().getName();
        }
        String base = result.getTestClass().getRealClass().getSimpleName() + "#" + result.getMethod().getMethodName();
        if (params == null || params.length == 0) {
            return base;
        }
        return base + Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
    }
}
