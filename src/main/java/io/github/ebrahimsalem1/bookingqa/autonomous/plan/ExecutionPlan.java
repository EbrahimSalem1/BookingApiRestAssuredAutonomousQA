package io.github.ebrahimsalem1.bookingqa.autonomous.plan;

import java.util.List;
import java.util.Map;

/**
 * The agent's decision for this run, with the reason for every decision so a human can audit it.
 *
 * @param surfaceHealth  health per surface (TestNG group) derived from exploration
 * @param includedGroups TestNG groups that will run
 * @param excludedGroups TestNG group -> why it was excluded
 * @param cucumberTags   Cucumber tag expression mirroring the exclusions
 * @param riskRanking    product areas ordered by risk, highest first
 * @param attention      notable observations the reviewer should look at first
 */
public record ExecutionPlan(Map<String, SurfaceHealth> surfaceHealth,
                            List<String> includedGroups,
                            Map<String, String> excludedGroups,
                            String cucumberTags,
                            List<String> riskRanking,
                            List<String> attention) {

    public enum SurfaceHealth { HEALTHY, DEGRADED, BLOCKED, UNREACHABLE, NOT_PROBED }
}
