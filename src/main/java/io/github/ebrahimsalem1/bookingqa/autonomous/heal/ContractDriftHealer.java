package io.github.ebrahimsalem1.bookingqa.autonomous.heal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.ebrahimsalem1.bookingqa.autonomous.explore.Observation;
import io.github.ebrahimsalem1.bookingqa.config.QaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Self-healing for contracts, with a human in the loop.
 *
 * <p>When a JSON surface has no recorded shape yet, or its shape changed, the healer writes a
 * <i>proposed</i> baseline to {@code target/autonomous/healing}. It only overwrites the committed
 * baseline when explicitly told to ({@code -Dagent.heal.accept=true}), so a breaking change on
 * booking.com's side can never silently "heal" the suite into accepting it.
 */
public class ContractDriftHealer {

    private static final Logger LOG = LoggerFactory.getLogger(ContractDriftHealer.class);
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public List<HealingProposal> propose(List<Observation> observations, Path outputDir) {
        QaConfig cfg = QaConfig.get();
        String env = cfg.environment().key();
        boolean accept = cfg.boolValue("agent.heal.accept");
        List<HealingProposal> proposals = new ArrayList<>();

        for (Observation o : observations) {
            boolean usable = o.shape() != null && !o.shape().isEmpty() && !o.shape().contains("$:not-json")
                    && o.blockedReason() == null && o.status() != null;
            if (!usable) {
                continue;
            }
            String reason;
            if (!o.drift().baselineFound()) {
                reason = "no baseline recorded yet";
            } else if (o.drift().drifted()) {
                reason = (o.drift().breaking() ? "BREAKING drift, removed " + o.drift().removed() : "additive drift")
                        + (o.drift().added().isEmpty() ? "" : ", added " + o.drift().added());
            } else {
                continue;
            }
            try {
                Path proposed = outputDir.resolve("healing").resolve(env).resolve(o.probe() + ".json");
                Files.createDirectories(proposed.getParent());
                JSON.writeValue(proposed.toFile(), o.shape());
                boolean accepted = false;
                if (accept) {
                    Path committed = Path.of("src/main/resources/autonomous/baselines", env, o.probe() + ".json");
                    Files.createDirectories(committed.getParent());
                    JSON.writeValue(committed.toFile(), o.shape());
                    accepted = true;
                }
                proposals.add(new HealingProposal(o.probe(), reason, proposed.toString(), accepted));
                LOG.info("Healing proposal for {}: {} (accepted={})", o.probe(), reason, accepted);
            } catch (IOException e) {
                LOG.warn("Cannot write healing proposal for {}: {}", o.probe(), e.getMessage());
            }
        }
        return proposals;
    }

    public record HealingProposal(String probe, String reason, String proposedFile, boolean accepted) {
    }
}
