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
     * Recent, hive-local task assignments. Population is an estimate derived
     * from active assignments, not an extra world scan or an assumed hive census.
     */
    public record Workforce(int estimatedPopulation, int forageWorkers,
                            int nurseWorkers, int guardWorkers,
                            int buildWorkers, int restWorkers) {
        public Workforce {
            if (estimatedPopulation < 1 || forageWorkers < 0 || nurseWorkers < 0
                    || guardWorkers < 0 || buildWorkers < 0 || restWorkers < 0) {
                throw new IllegalArgumentException("invalid workforce snapshot");
            }
        }

        public int workers(Task task) {
            if (task == null) return 0;
            return switch (task) {
                case FORAGE -> forageWorkers;
                case NURSE -> nurseWorkers;
                case GUARD -> guardWorkers;
                case BUILD -> buildWorkers;
                case REST -> restWorkers;
            };
        }
    }

    /**
     * Selects the highest-scoring task. Switching carries a penalty to avoid
     * oscillation. The optional workforce snapshot saturates overstaffed jobs:
     * new workers prefer unmet tasks while incumbents receive a softer penalty.
     */
    public static Task select(Needs needs, Worker worker, Task previous, double switchPenalty) {
        return select(needs, worker, previous, switchPenalty, null);
    }

    public static Task select(Needs needs, Worker worker, Task previous,
                              double switchPenalty, Workforce workforce) {
        Objects.requireNonNull(needs);
        Objects.requireNonNull(worker);
        Objects.requireNonNull(previous);
        if (!Double.isFinite(switchPenalty) || switchPenalty < 0 || switchPenalty > 1) {
            throw new IllegalArgumentException("switchPenalty must be in [0,1]");
        }

        Task selected = previous;
        double best = adjustedScore(previous, needs, worker, previous, workforce);
        for (Task task : Task.values()) {
            double candidate = adjustedScore(task, needs, worker, previous, workforce)
                    - (task == previous ? 0 : switchPenalty);
            if (candidate > best) {
                best = candidate;
                selected = task;
            }
        }
        return selected;
    }

    /**
     * Demand-derived local headcount target. Zero-demand tasks have no target;
     * all active tasks retain at least one slot in a non-empty workforce.
     */
    public static int desiredHeadcount(Task task, Needs needs, int population) {
        Objects.requireNonNull(needs);
        if (task == null || population < 1) {
            throw new IllegalArgumentException("invalid task headcount request");
        }

        double need;
        double share;
        switch (task) {
            case FORAGE -> {
                need = needs.nectarShortage();
                share = 0.35 + 0.45 * need;
            }
            case NURSE -> {
                need = needs.broodDemand();
                share = 0.05 + 0.20 * need;
            }
            case GUARD -> {
                need = needs.threat();
                share = 0.05 + 0.20 * need;
            }
            case BUILD -> {
                need = needs.spaceShortage();
                share = 0.05 + 0.15 * need;
            }
            case REST -> {
                need = needs.fatigue();
                share = 0.05 + 0.20 * need;
            }
            default -> throw new IllegalStateException("Unhandled bee task: " + task);
        }
        if (need <= 0.0) return 0;
        return Math.min(population,
                Math.max(1, (int) Math.ceil(population * share)));
    }

    private static double adjustedScore(Task task, Needs needs, Worker worker,
                                        Task previous, Workforce workforce) {
        double base = score(task, needs, worker);
        if (workforce == null) return base;

        int desired = desiredHeadcount(task, needs, workforce.estimatedPopulation());
        if (desired <= 0) return base;
        int assigned = workforce.workers(task);
        if (assigned < desired) return base;

        // Keep a mild incumbent preference, but strongly discourage adding
        // another worker after the local target has already been filled.
        double saturation = task == previous ? 0.72 : 0.18;
        int excess = Math.max(0, assigned - desired);
        return base * saturation / (1.0 + 0.35 * excess);
    }

    private static double score(Task task, Needs n, Worker w) {
        return switch (task) {
            case FORAGE -> n.nectarShortage() * (0.5 + 0.5 * w.foragingBias());
            case NURSE -> n.broodDemand() * (0.5 + 0.5 * w.nursingBias());
            case GUARD -> n.threat() * (1.15 + 0.45 * w.guardingBias());
            case BUILD -> n.spaceShortage() * (0.5 + 0.5 * w.buildingBias());
            // Rest is deliberately a strong but bounded response to repeated trips.
            case REST -> n.fatigue() * 1.25;
        };
    }
}