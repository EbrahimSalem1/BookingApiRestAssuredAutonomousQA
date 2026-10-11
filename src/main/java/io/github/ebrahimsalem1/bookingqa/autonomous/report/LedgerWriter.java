package io.github.ebrahimsalem1.bookingqa.autonomous.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.RunLedger;
import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.TestOutcome;
import io.github.ebrahimsalem1.bookingqa.autonomous.triage.FailureCategory;
import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Writes the run ledger (JSON) and a triage summary (Markdown) for plain {@code mvn test} runs. */
public final class LedgerWriter {

    private static final Logger LOG = LoggerFactory.getLogger(LedgerWriter.class);
    static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private LedgerWriter() {
    }

    public static Path outputDir() {
        Path dir = Path.of(QaConfig.get().require("agent.output.dir"));
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create " + dir, e);
        }
        return dir;
    }

    public static void write(List<TestOutcome> outcomes) {
        try {
            Path dir = outputDir();
            JSON.writeValue(dir.resolve("run-ledger.json").toFile(), outcomes);
            String md = "# Triage summary (" + QaConfig.get().environment().key() + ")\n\n"
                    + "**Verdict: " + Verdict.of(outcomes) + "**\n\n"
                    + outcomeTable(outcomes);
            Files.writeString(dir.resolve("triage-summary.md"), md, StandardCharsets.UTF_8);
            publishSummary(md);
            LOG.info("Verdict {} | {} | ledger at {}", Verdict.of(outcomes), RunLedger.countByStatus(outcomes), dir.toAbsolutePath());
        } catch (IOException e) {
            LOG.warn("Could not write ledger: {}", e.getMessage());
        }
    }

    static String outcomeTable(List<TestOutcome> outcomes) {
        StringBuilder sb = new StringBuilder();
        Map<TestOutcome.Status, Long> byStatus = RunLedger.countByStatus(outcomes);
        sb.append("| Passed | Failed | Skipped | Retried |\n|---|---|---|---|\n")
                .append("| ").append(byStatus.getOrDefault(TestOutcome.Status.PASSED, 0L))
                .append(" | ").append(byStatus.getOrDefault(TestOutcome.Status.FAILED, 0L))
                .append(" | ").append(byStatus.getOrDefault(TestOutcome.Status.SKIPPED, 0L))
                .append(" | ").append(byStatus.getOrDefault(TestOutcome.Status.RETRIED, 0L))
                .append(" |\n\n");

        List<TestOutcome> notPassed = outcomes.stream()
                .filter(o -> o.status() == TestOutcome.Status.FAILED || o.status() == TestOutcome.Status.SKIPPED)
                .sorted(Comparator.comparing((TestOutcome o) -> o.status() != TestOutcome.Status.FAILED)
                        .thenComparing(o -> o.category() == null ? FailureCategory.UNCLASSIFIED : o.category()))
                .toList();
        if (notPassed.isEmpty()) {
            return sb.append("All executed checks passed.\n").toString();
        }
        sb.append("| Status | Category | Blocking | Check | Reason |\n|---|---|---|---|---|\n");
        for (TestOutcome o : notPassed) {
            sb.append("| ").append(o.status())
                    .append(" | ").append(o.category())
                    .append(" | ").append(o.blocking() ? "yes" : "no")
                    .append(" | `").append(escape(o.id())).append('`')
                    .append(" | ").append(escape(o.reason()))
                    .append(" |\n");
        }
        return sb.toString();
    }

    static String escape(String s) {
        if (s == null) {
            return "";
        }
        String one = s.replace("|", "\\|").replace("\n", " ").replace("`", "'");
        return one.length() > 220 ? one.substring(0, 220) + "..." : one;
    }

    /** Appends Markdown to the GitHub Actions job summary when running in Actions. */
    public static void publishSummary(String markdown) {
        String summary = System.getenv("GITHUB_STEP_SUMMARY");
        if (summary == null || summary.isBlank()) {
            return;
        }
        try {
            Files.writeString(Path.of(summary), markdown + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.warn("Could not append GitHub step summary: {}", e.getMessage());
        }
    }
}
