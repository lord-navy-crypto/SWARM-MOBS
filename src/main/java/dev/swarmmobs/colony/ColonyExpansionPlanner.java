package dev.swarmmobs.colony;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Compact, deterministic, incremental placement planner for existing nest blocks.
 * It does not place blocks, spend resources, or load chunks.
 * A connected colony may grow indefinitely; only per-attempt work is bounded.
 */
public final class ColonyExpansionPlanner {
    private ColonyExpansionPlanner() {}

    public enum Shape { COMPACT, HORIZONTAL }

    /**
     * Chooses a face-connected candidate using a bounded, caller-supplied
     * subset of the colony. Prefer locations touching several colony blocks,
     * avoiding long thin tendrils. Unloaded/protected cells must be rejected
     * by validSite before scoring or placement.
     *
     * An adapter must rotate sampled frontier regions between attempts so that
     * distant parts of a large colony can also grow.
     */
    public static Optional<ConnectedColonyLayout.Cell> choose(
            Set<ConnectedColonyLayout.Cell> sampledCells,
            Predicate<ConnectedColonyLayout.Cell> validSite,
            Shape shape, long tieBreakSeed, int maxCandidates) {
        Objects.requireNonNull(sampledCells);
        Objects.requireNonNull(validSite);
        Objects.requireNonNull(shape);
        if (maxCandidates < 1) throw new IllegalArgumentException("maxCandidates");
        if (sampledCells.isEmpty()) return Optional.empty();

        Set<ConnectedColonyLayout.Cell> candidates = new HashSet<>();
        for (var cell : sampledCells) {
            for (var candidate : cell.neighbors()) {
                if (!sampledCells.contains(candidate)) candidates.add(candidate);
            }
        }

        // Do not invoke expensive world checks on more than maxCandidates.
        // The input sample must be bounded and spatially local by the adapter.
        var ordered = candidates.stream()
                .sorted(Comparator.comparingLong(
                        (ConnectedColonyLayout.Cell cell) -> stableHash(cell, tieBreakSeed))
                        .thenComparingInt(ConnectedColonyLayout.Cell::x)
                        .thenComparingInt(ConnectedColonyLayout.Cell::y)
                        .thenComparingInt(ConnectedColonyLayout.Cell::z))
                .limit(maxCandidates)
                .toList();

        ConnectedColonyLayout.Cell best = null;
        int bestScore = Integer.MIN_VALUE;
        for (var candidate : ordered) {
            if (!validSite.test(candidate)) continue;
            int touches = 0;
            for (var neighbor : candidate.neighbors()) {
                if (sampledCells.contains(neighbor)) touches++;
            }
            int score = touches * 10;
            if (shape == Shape.HORIZONTAL) {
                boolean sameHeightNeighbor = candidate.neighbors().stream()
                        .anyMatch(p -> p.y() == candidate.y() && sampledCells.contains(p));
                if (sameHeightNeighbor) score += 4;
            }
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return Optional.ofNullable(best);
    }

    private static long stableHash(ConnectedColonyLayout.Cell p, long seed) {
        long value = seed ^ (p.x() * 0x9E3779B97F4A7C15L)
                ^ (p.y() * 0xBF58476D1CE4E5B9L)
                ^ (p.z() * 0x94D049BB133111EBL);
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
