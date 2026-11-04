package dev.swarmmobs.algorithm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Pure target-memory selection and bounded multi-agent observation fusion.
 *
 * Relays retain their original observation timestamps. Fusion produces a
 * derived estimate anchored to the newest actual observation; it never
 * refreshes evidence age or reads hidden live target state.
 */
public final class TargetRelayPolicy {
    public record FusionResult(
            TargetObservation observation,
            int contributors,
            double maximumHorizontalDisagreement
    ) {
        public boolean fused() {
            return contributors > 1;
        }
    }

    private record SnapshotKey(
            long tick,
            long x,
            long y,
            long z,
            long forwardX,
            long forwardZ,
            long velocityX,
            long velocityZ
    ) {
        static SnapshotKey of(TargetObservation observation) {
            return new SnapshotKey(
                    observation.observationTick(),
                    Double.doubleToLongBits(observation.x()),
                    Double.doubleToLongBits(observation.y()),
                    Double.doubleToLongBits(observation.z()),
                    Double.doubleToLongBits(observation.forwardX()),
                    Double.doubleToLongBits(observation.forwardZ()),
                    Double.doubleToLongBits(observation.velocityX()),
                    Double.doubleToLongBits(observation.velocityZ())
            );
        }
    }

    public static Optional<TargetObservation> selectFreshest(
            long currentTick,
            int memoryTicks,
            List<TargetObservation> records
    ) {
        long maxAge = Math.max(0, memoryTicks);

        return records.stream()
                .filter(record -> record != null && record.targetId() != null)
                .filter(record -> {
                    long age = currentTick - record.observationTick();
                    return age >= 0 && age <= maxAge;
                })
                .max(Comparator.comparingLong(TargetObservation::observationTick));
    }

    /**
     * Fuse a small time-aligned cluster for the freshest target.
     *
     * Older snapshots are projected only across the configured short window
     * using their own observed velocity. Projection is capped at three blocks.
     * Spatial outliers and duplicate relays are excluded. The result retains
     * the newest source observation tick, so fusion cannot make stale data look
     * newly observed.
     */
    public static Optional<FusionResult> fuseFreshest(
            long currentTick,
            int memoryTicks,
            List<TargetObservation> records,
            int fusionWindowTicks,
            double maximumSpreadBlocks
    ) {
        if (records == null || records.isEmpty() || currentTick < 0) {
            return Optional.empty();
        }

        int horizon = Math.max(0, memoryTicks);
        int window = Math.max(0, fusionWindowTicks);
        double spreadLimit = Double.isFinite(maximumSpreadBlocks)
                ? Math.max(0.0, maximumSpreadBlocks) : 0.0;

        TargetObservation anchor = records.stream()
                .filter(TargetRelayPolicy::validPosition)
                .filter(record -> {
                    long age = currentTick - record.observationTick();
                    return age >= 0 && age <= horizon;
                })
                .max(Comparator.comparingLong(TargetObservation::observationTick))
                .orElse(null);
        if (anchor == null) return Optional.empty();

        List<TargetObservation> ordered = new ArrayList<>(records);
        ordered.sort(Comparator
                .comparingLong(TargetObservation::observationTick)
                .thenComparingDouble(TargetObservation::x)
                .thenComparingDouble(TargetObservation::y)
                .thenComparingDouble(TargetObservation::z));

        Set<SnapshotKey> seen = new HashSet<>();
        double weightSum = 0.0;
        double xSum = 0.0;
        double ySum = 0.0;
        double zSum = 0.0;
        double forwardXSum = 0.0;
        double forwardZSum = 0.0;
        double velocityXSum = 0.0;
        double velocityZSum = 0.0;
        double maxDisagreement = 0.0;
        int contributors = 0;

        for (TargetObservation record : ordered) {
            if (!validPosition(record) || !anchor.targetId().equals(record.targetId())) {
                continue;
            }
            long age = currentTick - record.observationTick();
            long delta = anchor.observationTick() - record.observationTick();
            if (age < 0 || age > horizon || delta < 0 || delta > window) {
                continue;
            }
            if (Math.abs(record.y() - anchor.y()) > 4.0) continue;
            if (!seen.add(SnapshotKey.of(record))) continue;

            double projectedX = record.x();
            double projectedZ = record.z();
            if (record.hasFiniteVelocity() && delta > 0) {
                double dx = record.velocityX() * delta;
                double dz = record.velocityZ() * delta;
                double correction = Math.hypot(dx, dz);
                if (correction > 3.0 && correction > 1.0e-9) {
                    double scale = 3.0 / correction;
                    dx *= scale;
                    dz *= scale;
                }
                projectedX += dx;
                projectedZ += dz;
            }

            double disagreement = Math.hypot(
                    projectedX - anchor.x(), projectedZ - anchor.z());
            if (!Double.isFinite(disagreement) || disagreement > spreadLimit) {
                continue;
            }

            double confidence = record.confidence(currentTick, horizon);
            double weight = confidence / (1.0 + delta);
            if (!Double.isFinite(weight) || weight <= 0.0) continue;

            weightSum += weight;
            xSum += projectedX * weight;
            ySum += record.y() * weight;
            zSum += projectedZ * weight;
            forwardXSum += finiteOr(record.forwardX(), finiteOr(anchor.forwardX(), 0.0)) * weight;
            forwardZSum += finiteOr(record.forwardZ(), finiteOr(anchor.forwardZ(), 0.0)) * weight;
            if (record.hasFiniteVelocity()) {
                velocityXSum += record.velocityX() * weight;
                velocityZSum += record.velocityZ() * weight;
            }
            maxDisagreement = Math.max(maxDisagreement, disagreement);
            contributors++;
        }

        if (contributors <= 1 || weightSum <= 1.0e-12) {
            return Optional.of(new FusionResult(anchor, 1, 0.0));
        }

        TargetObservation fused = new TargetObservation(
                anchor.targetId(),
                anchor.observationTick(),
                xSum / weightSum,
                ySum / weightSum,
                zSum / weightSum,
                forwardXSum / weightSum,
                forwardZSum / weightSum,
                velocityXSum / weightSum,
                velocityZSum / weightSum
        );
        return Optional.of(new FusionResult(fused, contributors, maxDisagreement));
    }

    private static double finiteOr(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private static boolean validPosition(TargetObservation record) {
        return record != null && record.targetId() != null
                && record.hasFinitePosition()
                && Double.isFinite(record.x())
                && Double.isFinite(record.y())
                && Double.isFinite(record.z());
    }

    private TargetRelayPolicy() {}
}
