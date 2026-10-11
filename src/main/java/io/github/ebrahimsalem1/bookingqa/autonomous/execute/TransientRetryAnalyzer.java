package io.github.ebrahimsalem1.bookingqa.autonomous.execute;

import io.github.ebrahimsalem1.bookingqa.autonomous.triage.FailureTriager;
import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.concurrent.TimeUnit;

/**
 * Retries a failed check only when triage says the failure is transient (network, 429,
 * 502/503/504). Real assertion failures are never retried: blind retries hide bugs.
 * Uses a short linear back-off so a throttled endpoint gets room to recover.
 */
public class TransientRetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(TransientRetryAnalyzer.class);
    private int attempts;

    @Override
    public boolean retry(ITestResult result) {
        var triage = FailureTriager.classify(result.getThrowable());
        int max = QaConfig.get().maxTransientRetries();
        if (!triage.category().retryable() || attempts >= max) {
            return false;
        }
        attempts++;
        LOG.warn("Retrying {} ({}/{}) because {}: {}", result.getName(), attempts, max, triage.category(), triage.reason());
        try {
            TimeUnit.SECONDS.sleep(2L * attempts);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }
}
