package io.github.ebrahimsalem1.bookingqa.autonomous.report;

import io.github.ebrahimsalem1.bookingqa.autonomous.explore.Observation;
import io.github.ebrahimsalem1.bookingqa.autonomous.heal.ContractDriftHealer.HealingProposal;
import io.github.ebrahimsalem1.bookingqa.autonomous.ledger.TestOutcome;
import io.github.ebrahimsalem1.bookingqa.autonomous.plan.ExecutionPlan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Everything one agent run saw, decided and concluded; rendered as JSON and Markdown. */
public record AgentReport(String environment,
                          String startedAt,
                          String finishedAt,
                          Verdict verdict,
                          String verdictReason,
                          List<Observation> observations,
                          ExecutionPlan plan,
                          List<String> phaseNotes,
                          List<TestOutcome> outcomes,
                          List<HealingProposal> healing,
                          String aiAdvice) {

    public void write(Path dir) throws IOException {
        Files.createDirectories(dir);
        LedgerWriter.JSON.writeValue(dir.resolve("report.json").toFile(), this);
        LedgerWriter.JSON.writeValue(dir.resolve("run-ledger.json").toFile(), outcomes);
        Files.writeString(dir.resolve("report.md"), toMarkdown(), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("verdict.txt"), verdict.name(), StandardCharsets.UTF_8);
    }

    /** Short input for the LLM advisor: no headers, no bodies, no secrets. */
    public String digest() {
        StringBuilder sb = new StringBuilder();
        sb.append("Environment: ").append(environment).append('\n');
        sb.append("Deterministic verdict: ").append(verdict).append(" - ").append(verdictReason).append("\n\n");
        sb.append("Excluded surfaces: ").append(plan.excludedGroups()).append("\n\n");
        sb.append("Explorer attention list:\n");
        plan.attention().forEach(a -> sb.append("- ").append(a).append('\n'));
        sb.append("\nTriage of non-passing checks:\n").append(LedgerWriter.outcomeTable(outcomes));
        return sb.toString();
    }

    public String toMarkdown() {
        StringBuilder md = new StringBuilder();
        String icon = switch (verdict) {
            case PASS -> "PASS";
            case INCONCLUSIVE -> "INCONCLUSIVE";
            case FAIL -> "FAIL";
        };
        md.append("# Autonomous QA run: booking.com (`").append(environment).append("`)\n\n")
                .append("**Verdict: ").append(icon).append("**: ").append(verdictReason).append("\n\n")
                .append("Started ").append(startedAt).append(", finished ").append(finishedAt).append(".\n\n");

        md.append("## 1. Observe: exploration probes\n\n")
                .append("| Probe | Area | Risk | Health | HTTP | Latency (ms) | Note |\n|---|---|---|---|---|---|---|\n");
        for (Observation o : observations) {
            md.append("| `").append(o.probe()).append("` | ").append(o.area()).append(" | ").append(o.risk())
                    .append(" | ").append(o.health()).append(" | ").append(o.status() == null ? "-" : o.status())
                    .append(" | ").append(o.latencyMs() == null ? "-" : o.latencyMs())
                    .append(" | ").append(LedgerWriter.escape(o.blockedReason() != null ? o.blockedReason() : (o.error() != null ? o.error() : o.note())))
                    .append(" |\n");
        }

        observations.stream().filter(o -> "front-door".equals(o.probe()) && !o.notableHeaders().isEmpty()).findFirst()
                .ifPresent(o -> {
                    md.append("\n<details><summary>Headers observed on the front door</summary>\n\n");
                    for (Map.Entry<String, String> h : o.notableHeaders().entrySet()) {
                        md.append("- `").append(h.getKey()).append("`: ").append(LedgerWriter.escape(h.getValue())).append('\n');
                    }
                    md.append("\n</details>\n");
                });

        md.append("\n## 2. Plan: decisions and reasons\n\n**Surface health:** ");
        plan.surfaceHealth().forEach((k, v) -> md.append('`').append(k).append("`=").append(v).append("  "));
        md.append("\n\n**Risk ranking:** ").append(String.join(" > ", plan.riskRanking())).append("\n\n");
        if (plan.excludedGroups().isEmpty()) {
            md.append("No surface excluded.\n");
        } else {
            md.append("| Excluded group | Why |\n|---|---|\n");
            plan.excludedGroups().forEach((g, why) -> md.append("| `").append(g).append("` | ").append(LedgerWriter.escape(why)).append(" |\n"));
        }
        if (!plan.attention().isEmpty()) {
            md.append("\n**Look here first:**\n\n");
            plan.attention().forEach(a -> md.append("- ").append(LedgerWriter.escape(a)).append('\n'));
        }

        md.append("\n## 3. Execute\n\n");
        phaseNotes.forEach(n -> md.append("- ").append(n).append('\n'));

        md.append("\n## 4. Triage\n\n").append(LedgerWriter.outcomeTable(outcomes));

        md.append("\n## 5. Heal: contract baselines\n\n");
        if (healing.isEmpty()) {
            md.append("No contract drift and no missing baselines.\n");
        } else {
            md.append("| Probe | Finding | Proposed baseline | Accepted |\n|---|---|---|---|\n");
            healing.forEach(h -> md.append("| `").append(h.probe()).append("` | ").append(LedgerWriter.escape(h.reason()))
                    .append(" | `").append(h.proposedFile()).append("` | ").append(h.accepted() ? "yes" : "no (review, then rerun with -Dagent.heal.accept=true)")
                    .append(" |\n"));
        }

        md.append("\n## 6. AI advisor (advisory only, does not change the verdict)\n\n")
                .append(aiAdvice == null || aiAdvice.isBlank() ? "_Not configured: set `ANTHROPIC_API_KEY` to enable._\n" : aiAdvice + "\n");
        return md.toString();
    }
}
