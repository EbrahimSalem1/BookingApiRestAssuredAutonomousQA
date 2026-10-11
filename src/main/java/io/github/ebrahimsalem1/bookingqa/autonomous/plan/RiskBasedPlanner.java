package io.github.ebrahimsalem1.bookingqa.autonomous.plan;

import io.github.ebrahimsalem1.bookingqa.autonomous.explore.Observation;
import io.github.ebrahimsalem1.bookingqa.autonomous.plan.ExecutionPlan.SurfaceHealth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Turns observations into an {@link ExecutionPlan}.
 *
 * <ul>
 *   <li>A surface the explorer could not reach, or that bot protection fully challenged, is
 *       excluded: running 20 checks that will all be inconclusive wastes time and buries signal.</li>
 *   <li>A partially challenged surface (DEGRADED) still runs: each check self-skips if challenged.</li>
 *   <li>Product areas are ranked by the summed risk of their probes, weighted up when the
 *       explorer saw something unusual (drift, unexpected status, slowness).</li>
 * </ul>
 */
public class RiskBasedPlanner {

    /** Probe target -> TestNG group / Cucumber tag of the checks that exercise that surface. */
    static final Map<String, String> TARGET_TO_GROUP = Map.of(
            "web", "web",
            "autocomplete", "autocomplete",
            "demand-anonymous", "demand-auth",
            "demand", "demand-partner");

    static final List<String> ALWAYS = List.of("unit", "smoke", "cucumber");

    public ExecutionPlan plan(List<Observation> observations) {
        Map<String, List<Observation>> byGroup = observations.stream()
                .collect(Collectors.groupingBy(o -> TARGET_TO_GROUP.getOrDefault(o.target(), o.target()), LinkedHashMap::new, Collectors.toList()));

        Map<String, SurfaceHealth> health = new LinkedHashMap<>();
        Map<String, String> excluded = new LinkedHashMap<>();
        List<String> included = new ArrayList<>(ALWAYS);

        for (var entry : byGroup.entrySet()) {
            SurfaceHealth h = aggregate(entry.getValue());
            health.put(entry.getKey(), h);
            switch (h) {
                case UNREACHABLE -> excluded.put(entry.getKey(), "unreachable from this network: " + firstNote(entry.getValue()));
                case BLOCKED -> excluded.put(entry.getKey(), "every probe was challenged by bot protection: " + firstBlock(entry.getValue()));
                case NOT_PROBED -> excluded.put(entry.getKey(), firstNote(entry.getValue()));
                default -> included.add(entry.getKey());
            }
        }
        // groups that exist in the suite but have no probe still run
        for (String g : List.of("search", "checkout", "contract", "negative", "security", "performance")) {
            if (!included.contains(g)) {
                included.add(g);
            }
        }

        String tags = excluded.keySet().stream().map(g -> "not @" + g).collect(Collectors.joining(" and "));

        Map<String, Integer> areaRisk = new LinkedHashMap<>();
        for (Observation o : observations) {
            int weight = o.risk() * (o.health() == Observation.Health.HEALTHY || o.health() == Observation.Health.NOT_PROBED ? 1 : 2);
            areaRisk.merge(o.area(), weight, Integer::sum);
        }
        List<String> ranking = areaRisk.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> e.getKey() + " (risk score " + e.getValue() + ")")
                .toList();

        List<String> attention = observations.stream()
                .filter(o -> o.health() != Observation.Health.HEALTHY && o.health() != Observation.Health.NOT_PROBED)
                .sorted(Comparator.comparingInt(Observation::risk).reversed())
                .map(o -> "[" + o.health() + "] " + o.probe() + " (risk " + o.risk() + "): " + o.note())
                .toList();

        return new ExecutionPlan(health, included, excluded, tags, ranking, attention);
    }

    static SurfaceHealth aggregate(List<Observation> obs) {
        if (obs.stream().allMatch(o -> o.health() == Observation.Health.NOT_PROBED)) {
            return SurfaceHealth.NOT_PROBED;
        }
        if (obs.stream().anyMatch(o -> o.health() == Observation.Health.UNREACHABLE)) {
            return SurfaceHealth.UNREACHABLE;
        }
        long blocked = obs.stream().filter(o -> o.health() == Observation.Health.BLOCKED).count();
        if (blocked == obs.size()) {
            return SurfaceHealth.BLOCKED;
        }
        return blocked > 0 ? SurfaceHealth.DEGRADED : SurfaceHealth.HEALTHY;
    }

    private static String firstNote(List<Observation> obs) {
        return obs.stream().map(o -> o.error() != null ? o.error() : o.note()).filter(s -> s != null && !s.isBlank()).findFirst().orElse("n/a");
    }

    private static String firstBlock(List<Observation> obs) {
        return obs.stream().map(Observation::blockedReason).filter(s -> s != null).findFirst().orElse("n/a");
    }
}
