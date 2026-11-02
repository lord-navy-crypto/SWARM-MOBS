package dev.swarmmobs.bee;

import java.util.Objects;

/**
 * Deterministic, Minecraft-independent task selection for honeybee workers.
 * Colony-level signals are observations, not commands from an omniscient queen.
 */
public final class BeeTaskPolicy {
    private BeeTaskPolicy() {}

    public enum Task { FORAGE, NURSE, GUARD, BUILD, REST }

    public record Needs(double nectarShortage, double broodDemand,
                        double threat, double spaceShortage, double fatigue) {
        public Needs {
            check(nectarShortage);
            check(broodDemand);
            check(threat);
            check(spaceShortage);
            check(fatigue);
        }
        private static void check(double value) {
            if (!Double.isFinite(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("Needs must be finite in [0,1]");
            }
        }
    }

    public record Worker(double foragingBias, double nursingBias,
                         double guardingBias, double buildingBias) {
        public Worker {
            check(foragingBias); check(nursingBias);
            check(guardingBias); check(buildingBias);
        }
        private static void check(double value) {
            if (!Double.isFinite(value) || value < 0 || value > 1) {
                throw new IllegalArgumentException("Worker biases must be finite in [0,1]");
            }
        }
    }

    /**
     * Selects the highest-scoring task. Switching carries a penalty to avoid
     * per-tick oscillation. No shared global target or mutable random source.
     */
    public static Task select(Needs needs, Worker worker, Task previous, double switchPenalty) {
        Objects.requireNonNull(needs);
        Objects.requireNonNull(worker);
        Objects.requireNonNull(previous);
        if (!Double.isFinite(switchPenalty) || switchPenalty < 0 || switchPenalty > 1) {
            throw new IllegalArgumentException("switchPenalty must be in [0,1]");
        }
        Task selected = previous;
        double best = score(previous, needs, worker);
        for (Task task : Task.values()) {
            double candidate = score(task, needs, worker)
                    - (task == previous ? 0 : switchPenalty);
            if (candidate > best) {
                best = candidate;
                selected = task;
            }
        }
        return selected;
    }

    private static double score(Task task, Needs n, Worker w) {
        return switch (task) {
            case FORAGE -> n.nectarShortage() * (0.5 + 0.5 * w.foragingBias());
            case NURSE -> n.broodDemand() * (0.5 + 0.5 * w.nursingBias());
            case GUARD -> n.threat() * (0.5 + 0.5 * w.guardingBias());
            case BUILD -> n.spaceShortage() * (0.5 + 0.5 * w.buildingBias());
            case REST -> n.fatigue();
        };
    }
}
