package dev.swarmmobs.ai;

import dev.swarmmobs.ai.SwarmAiExperienceTransfer.RunSample;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwarmAiExperienceTransferTest {

    @Test
    void derivesModerateEvidenceFromThreeComparableSameSeedPairs() {
        List<RunSample> runs = new ArrayList<>();
        for (int seed = 11; seed <= 13; seed++) {
            runs.add(run(seed * 10L, "BASELINE", seed, 8, 1200L,
                    100L, 5L, 10L, 1L, 8L, 2L, 80L, 10L, 9L));
            runs.add(run(seed * 10L + 1L, "NOISY_SENSING", seed, 8, 1180L,
                    100L, 25L, 10L, 4L, 5L, 5L, 120L, 10L, 6L));
        }

        var evidence = SwarmAiExperienceTransfer.analyze(runs).stream()
                .filter(item -> item.factor().equals("SENSING"))
                .filter(item -> item.metric().equals("COMMUNICATION_DROP_RATE"))
                .findFirst()
                .orElseThrow();

        assertEquals(3, evidence.matchedSeedPairs());
        assertEquals(3, evidence.consistentPairs());
        assertEquals(SwarmAiExperienceTransfer.Confidence.MEDIUM, evidence.confidence());
        assertEquals(SwarmAiExperienceTransfer.Effect.REGRESSED, evidence.effect());
        assertTrue(evidence.isTransferCandidate());
        assertEquals(List.of(110L, 120L, 130L), evidence.controlRunIds());
        assertEquals(List.of(111L, 121L, 131L), evidence.treatmentRunIds());
    }

    @Test
    void doesNotPairRunsWithDifferentRandomSeeds() {
        List<RunSample> runs = List.of(
                run(1L, "BASELINE", 7, 8, 1000L, 100L, 5L, 10L, 1L, 8L, 2L, 80L, 10L, 9L),
                run(2L, "NOISY_SENSING", 8, 8, 1000L, 100L, 25L, 10L, 4L, 5L, 5L, 120L, 10L, 6L)
        );

        assertTrue(SwarmAiExperienceTransfer.analyze(runs).isEmpty());
    }

    @Test
    void refusesToTreatCombinedFaultsAsASingleFactorComparisonWithBaseline() {
        List<RunSample> runs = List.of(
                run(1L, "BASELINE", 42, 8, 1000L, 100L, 5L, 10L, 1L, 8L, 2L, 80L, 10L, 9L),
                run(2L, "COMBINED_FAULTS", 42, 8, 1000L, 100L, 25L, 10L, 4L, 5L, 5L, 120L, 10L, 6L)
        );

        assertTrue(SwarmAiExperienceTransfer.analyze(runs).isEmpty());
    }

    @Test
    void countsRepeatedPairsFromOneSeedAsOneIndependentReplication() {
        List<RunSample> runs = List.of(
                run(1L, "BASELINE", 42, 8, 1000L, 100L, 5L, 10L, 1L, 8L, 2L, 80L, 10L, 9L),
                run(2L, "NOISY_SENSING", 42, 8, 1000L, 100L, 25L, 10L, 4L, 5L, 5L, 120L, 10L, 6L),
                run(3L, "BASELINE", 42, 8, 1020L, 100L, 5L, 10L, 1L, 8L, 2L, 80L, 10L, 9L),
                run(4L, "NOISY_SENSING", 42, 8, 1010L, 100L, 25L, 10L, 4L, 5L, 5L, 120L, 10L, 6L)
        );

        var evidence = SwarmAiExperienceTransfer.analyze(runs).stream()
                .filter(item -> item.metric().equals("COMMUNICATION_DROP_RATE"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, evidence.matchedSeedPairs());
        assertEquals(SwarmAiExperienceTransfer.Confidence.LOW, evidence.confidence());
    }

    @Test
    void rejectsPairsWithMateriallyDifferentPopulationOrDuration() {
        List<RunSample> runs = List.of(
                run(1L, "BASELINE", 42, 4, 1000L, 100L, 5L, 10L, 1L, 8L, 2L, 80L, 10L, 9L),
                run(2L, "NOISY_SENSING", 42, 20, 1000L, 100L, 25L, 10L, 4L, 5L, 5L, 120L, 10L, 6L),
                run(3L, "BASELINE", 43, 8, 200L, 100L, 5L, 10L, 1L, 8L, 2L, 80L, 10L, 9L),
                run(4L, "NOISY_SENSING", 43, 8, 1000L, 100L, 25L, 10L, 4L, 5L, 5L, 120L, 10L, 6L)
        );

        assertTrue(SwarmAiExperienceTransfer.analyze(runs).isEmpty());
    }

    private static RunSample run(
            long id, String preset, int seed, int agents, long ticks,
            long delivered, long dropped, long recoveryAttempts, long recoveryFailures,
            long searchSucceeded, long searchFailed, long reacquisitionTicks,
            long engineeringClaims, long engineeringCompleted
    ) {
        return new RunSample(
                id, id * 100L, preset, seed, ticks, agents,
                delivered, dropped, recoveryAttempts, recoveryFailures,
                searchSucceeded, searchFailed, reacquisitionTicks,
                engineeringClaims, engineeringCompleted
        );
    }
}
