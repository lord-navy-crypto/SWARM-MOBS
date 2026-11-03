package dev.swarmmobs.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Derives auditable cross-experiment evidence from saved aggregate run records.
 * This class never changes runtime configuration.
 */
public final class SwarmAiExperienceTransfer {
    private static final double MAX_AGENT_COUNT_MISMATCH = 0.25;
    private static final double MAX_DURATION_MISMATCH = 0.20;
    private static final int MAX_INSIGHTS = 15;

    private static final List<Comparison> COMPARISONS = List.of(
            new Comparison("SENSING", "BASELINE", "NOISY_SENSING"),
            new Comparison("SENSING", "LOSSY_COMMS", "COMBINED_FAULTS"),
            new Comparison("COMMUNICATION", "BASELINE", "LOSSY_COMMS"),
            new Comparison("COMMUNICATION", "NOISY_SENSING", "COMBINED_FAULTS"),
            new Comparison("NAVIGATION", "BASELINE", "NAVIGATION_STRESS")
    );

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    public enum Effect {
        IMPROVED,
        REGRESSED,
        NO_CLEAR_CHANGE,
        MIXED
    }

    private enum Metric {
        COMMUNICATION_DROP_RATE(false, 0.02, 20L) {
            @Override double value(RunSample run) { return run.communicationDropRate(); }
            @Override long samples(RunSample run) {
                return run.communicationDelivered() + run.communicationDropped();
            }
        },
        RECOVERY_FAILURE_RATE(false, 0.02, 5L) {
            @Override double value(RunSample run) { return run.recoveryFailureRate(); }
            @Override long samples(RunSample run) { return run.recoveryAttempts(); }
        },
        SEARCH_SUCCESS_RATE(true, 0.02, 3L) {
            @Override double value(RunSample run) { return run.searchSuccessRate(); }
            @Override long samples(RunSample run) {
                return run.searchSucceeded() + run.searchFailed();
            }
        },
        AVERAGE_REACQUISITION_TICKS(false, 2.0, 3L) {
            @Override double value(RunSample run) { return run.averageReacquisitionTicks(); }
            @Override long samples(RunSample run) { return run.searchSucceeded(); }
        },
        ENGINEERING_TASK_COMPLETION_RATE(true, 0.02, 3L) {
            @Override double value(RunSample run) { return run.engineeringTaskCompletionRate(); }
            @Override long samples(RunSample run) { return run.engineeringClaims(); }
        };

        private final boolean higherIsBetter;
        private final double epsilon;
        private final long minimumSamples;

        Metric(boolean higherIsBetter, double epsilon, long minimumSamples) {
            this.higherIsBetter = higherIsBetter;
            this.epsilon = epsilon;
            this.minimumSamples = minimumSamples;
        }

        abstract double value(RunSample run);
        abstract long samples(RunSample run);
    }

    /**
     * Lightweight immutable projection of aggregate fields used by transfer analysis.
     * It intentionally has no Minecraft or persistence-layer dependency.
     */
    public record RunSample(
            long id,
            long gameTick,
            String preset,
            int seed,
            long elapsedTicks,
            int agentCount,
            long communicationDelivered,
            long communicationDropped,
            long recoveryAttempts,
            long recoveryFailures,
            long searchSucceeded,
            long searchFailed,
            long searchReacquisitionTicks,
            long engineeringClaims,
            long engineeringCompleted
    ) {
        public RunSample {
            preset = preset == null ? "" : preset;
        }

        public double communicationDropRate() {
            return ratio(communicationDropped, communicationDelivered + communicationDropped);
        }

        public double recoveryFailureRate() {
            return ratio(recoveryFailures, recoveryAttempts);
        }

        public double searchSuccessRate() {
            return ratio(searchSucceeded, searchSucceeded + searchFailed);
        }

        public double averageReacquisitionTicks() {
            return searchSucceeded <= 0L ? 0.0 : (double) searchReacquisitionTicks / searchSucceeded;
        }

        public double engineeringTaskCompletionRate() {
            return ratio(engineeringCompleted, engineeringClaims);
        }
    }

    private static double ratio(long numerator, long denominator) {
        return denominator <= 0L ? 0.0 : (double) numerator / denominator;
    }

    public record Insight(
            String factor,
            String metric,
            String controlPreset,
            String treatmentPreset,
            double controlMedian,
            double treatmentMedian,
            double medianDelta,
            int matchedSeedPairs,
            int consistentPairs,
            long supportingSamples,
            Confidence confidence,
            Effect effect,
            List<Long> controlRunIds,
            List<Long> treatmentRunIds
    ) {
        public Insight {
            controlRunIds = controlRunIds == null ? List.of() : List.copyOf(controlRunIds);
            treatmentRunIds = treatmentRunIds == null ? List.of() : List.copyOf(treatmentRunIds);
        }

        public boolean isTransferCandidate() {
            return confidence != Confidence.LOW
                    && effect != Effect.MIXED
                    && effect != Effect.NO_CLEAR_CHANGE;
        }

        public String displayLine() {
            return String.format(
                    Locale.ROOT,
                    "%s/%s %s -> %s; delta=%+.3f; effect=%s; pairedSeeds=%d; consistency=%d/%d; samples=%d; confidence=%s; runs=%s/%s",
                    factor, metric, controlPreset, treatmentPreset, medianDelta, effect,
                    matchedSeedPairs, consistentPairs, matchedSeedPairs, supportingSamples,
                    confidence, controlRunIds, treatmentRunIds
            );
        }
    }

    public static List<Insight> analyze(List<RunSample> source) {
        if (source == null || source.size() < 2) {
            return List.of();
        }

        List<RunSample> runs = source.stream().filter(Objects::nonNull).toList();
        List<Insight> insights = new ArrayList<>();

        for (Comparison comparison : COMPARISONS) {
            List<RunPair> pairs = matchPairs(runs, comparison);
            if (pairs.isEmpty()) {
                continue;
            }

            for (Metric metric : Metric.values()) {
                List<Observation> observations = new ArrayList<>();
                for (RunPair pair : pairs) {
                    long controlSamples = metric.samples(pair.control());
                    long treatmentSamples = metric.samples(pair.treatment());
                    if (controlSamples < metric.minimumSamples
                            || treatmentSamples < metric.minimumSamples) {
                        continue;
                    }

                    double controlValue = metric.value(pair.control());
                    double treatmentValue = metric.value(pair.treatment());
                    if (!Double.isFinite(controlValue) || !Double.isFinite(treatmentValue)) {
                        continue;
                    }

                    observations.add(new Observation(
                            pair,
                            controlValue,
                            treatmentValue,
                            treatmentValue - controlValue,
                            Math.min(controlSamples, treatmentSamples)
                    ));
                }

                if (observations.isEmpty()) {
                    continue;
                }

                double controlMedian = median(observations.stream()
                        .map(Observation::controlValue).toList());
                double treatmentMedian = median(observations.stream()
                        .map(Observation::treatmentValue).toList());
                double medianDelta = median(observations.stream()
                        .map(Observation::delta).toList());

                int consistentPairs = 0;
                long supportingSamples = 0L;
                List<Long> controlIds = new ArrayList<>();
                List<Long> treatmentIds = new ArrayList<>();
                for (Observation observation : observations) {
                    if (consistent(observation.delta(), medianDelta, metric.epsilon)) {
                        consistentPairs++;
                    }
                    supportingSamples += observation.samples();
                    controlIds.add(observation.pair().control().id());
                    treatmentIds.add(observation.pair().treatment().id());
                }

                double consistency = (double) consistentPairs / observations.size();
                Confidence confidence = confidence(
                        observations.size(), consistency, supportingSamples
                );
                Effect effect = effect(metric, medianDelta, consistency);

                insights.add(new Insight(
                        comparison.factor(),
                        metric.name(),
                        comparison.controlPreset(),
                        comparison.treatmentPreset(),
                        controlMedian,
                        treatmentMedian,
                        medianDelta,
                        observations.size(),
                        consistentPairs,
                        supportingSamples,
                        confidence,
                        effect,
                        controlIds,
                        treatmentIds
                ));
            }
        }

        insights.sort(
                Comparator.comparingInt((Insight item) -> item.confidence().ordinal()).reversed()
                        .thenComparing(Comparator.comparingInt(Insight::matchedSeedPairs).reversed())
                        .thenComparing(Comparator.comparingLong(Insight::supportingSamples).reversed())
        );
        return List.copyOf(insights.subList(0, Math.min(MAX_INSIGHTS, insights.size())));
    }

    private static List<RunPair> matchPairs(List<RunSample> runs, Comparison comparison) {
        Map<Integer, List<RunSample>> bySeed = new HashMap<>();
        for (RunSample run : runs) {
            if (run.elapsedTicks() < 200L || run.agentCount() <= 0) {
                continue;
            }
            if (comparison.controlPreset().equals(run.preset())
                    || comparison.treatmentPreset().equals(run.preset())) {
                bySeed.computeIfAbsent(run.seed(), ignored -> new ArrayList<>()).add(run);
            }
        }

        List<Integer> seeds = new ArrayList<>(bySeed.keySet());
        seeds.sort(Integer::compareTo);
        List<RunPair> result = new ArrayList<>();

        for (int seed : seeds) {
            List<RunSample> controls = bySeed.get(seed).stream()
                    .filter(run -> comparison.controlPreset().equals(run.preset()))
                    .toList();
            List<RunSample> treatments = bySeed.get(seed).stream()
                    .filter(run -> comparison.treatmentPreset().equals(run.preset()))
                    .toList();

            RunPair best = null;
            double bestDistance = Double.MAX_VALUE;
            long bestRecency = Long.MIN_VALUE;
            for (RunSample control : controls) {
                for (RunSample treatment : treatments) {
                    if (!comparable(control, treatment)) {
                        continue;
                    }
                    double distance = mismatch(control, treatment);
                    long recency = Math.max(control.gameTick(), treatment.gameTick());
                    if (distance < bestDistance - 1.0e-9
                            || (Math.abs(distance - bestDistance) <= 1.0e-9
                            && recency > bestRecency)) {
                        best = new RunPair(control, treatment);
                        bestDistance = distance;
                        bestRecency = recency;
                    }
                }
            }
            // One pair per seed prevents repeated runs with one seed from
            // masquerading as independent replications.
            if (best != null) {
                result.add(best);
            }
        }
        return List.copyOf(result);
    }

    private static boolean comparable(RunSample first, RunSample second) {
        double agentMismatch = (double) Math.abs(first.agentCount() - second.agentCount())
                / Math.max(first.agentCount(), second.agentCount());
        double durationMismatch = (double) Math.abs(first.elapsedTicks() - second.elapsedTicks())
                / Math.max(first.elapsedTicks(), second.elapsedTicks());
        return agentMismatch <= MAX_AGENT_COUNT_MISMATCH
                && durationMismatch <= MAX_DURATION_MISMATCH;
    }

    private static double mismatch(RunSample first, RunSample second) {
        double agentMismatch = (double) Math.abs(first.agentCount() - second.agentCount())
                / Math.max(first.agentCount(), second.agentCount());
        double durationMismatch = (double) Math.abs(first.elapsedTicks() - second.elapsedTicks())
                / Math.max(first.elapsedTicks(), second.elapsedTicks());
        return agentMismatch + durationMismatch;
    }

    private static boolean consistent(double delta, double medianDelta, double epsilon) {
        if (medianDelta > epsilon) return delta > epsilon;
        if (medianDelta < -epsilon) return delta < -epsilon;
        return Math.abs(delta) <= epsilon;
    }

    private static Confidence confidence(int pairedSeeds, double consistency, long samples) {
        if (pairedSeeds >= 5 && consistency >= 0.80 && samples >= 40L) {
            return Confidence.HIGH;
        }
        if (pairedSeeds >= 3 && consistency >= 0.67 && samples >= 15L) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }

    private static Effect effect(Metric metric, double delta, double consistency) {
        if (consistency < 0.67) return Effect.MIXED;
        if (Math.abs(delta) <= metric.epsilon) return Effect.NO_CLEAR_CHANGE;
        boolean improved = metric.higherIsBetter ? delta > 0.0 : delta < 0.0;
        return improved ? Effect.IMPROVED : Effect.REGRESSED;
    }

    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int middle = sorted.size() / 2;
        if ((sorted.size() & 1) == 1) return sorted.get(middle);
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private record Comparison(String factor, String controlPreset, String treatmentPreset) {}
    private record RunPair(RunSample control, RunSample treatment) {}
    private record Observation(
            RunPair pair,
            double controlValue,
            double treatmentValue,
            double delta,
            long samples
    ) {}

    private SwarmAiExperienceTransfer() {}
}
