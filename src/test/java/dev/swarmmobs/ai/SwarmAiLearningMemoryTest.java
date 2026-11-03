package dev.swarmmobs.ai;

import dev.swarmmobs.experiment.SwarmExperimentMetrics;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwarmAiLearningMemoryTest {

    @Test
    void storesBoundedExperimentHistoryAndWeightedSummary() {
        SwarmAiLearningMemory memory = new SwarmAiLearningMemory();
        memory.record(snapshot(200L, 8, 80L, 20L, 4L, 1L, 3L, 1L, 60L, 4L, 3L),
                "BASELINE", 42, 1000L);
        memory.record(snapshot(400L, 10, 95L, 5L, 5L, 1L, 5L, 1L, 100L, 5L, 4L),
                "LOSSY_COMMS", 42, 1400L);

        var summary = memory.summary();
        assertEquals(2, summary.runCount());
        assertEquals(2, summary.presetCount());
        assertEquals(25.0 / 200.0, summary.communicationDropRate(), 1.0e-9);
        assertEquals(2.0 / 9.0, summary.recoveryFailureRate(), 1.0e-9);
        assertEquals(7.0 / 9.0, summary.searchSuccessRate(), 1.0e-9);
        assertEquals(160.0 / 7.0, summary.averageReacquisitionTicks(), 1.0e-9);
        assertEquals(7.0 / 9.0, summary.engineeringTaskCompletionRate(), 1.0e-9);
    }

    @Test
    void persistsRunsAndEvaluationAcrossNbtRoundTrip() {
        SwarmAiLearningMemory memory = new SwarmAiLearningMemory();
        memory.record(snapshot(300L, 6, 90L, 10L, 4L, 1L, 4L, 1L, 40L, 4L, 3L),
                "BASELINE", 7, 3000L);
        memory.record(snapshot(600L, 6, 85L, 15L, 5L, 1L, 5L, 0L, 50L, 5L, 5L),
                "NOISY_SENSING", 7, 3600L);
        memory.storeEvaluation(
                new SwarmAiEvaluation(
                        "Two runs show a cautious improvement in search completion.",
                        SwarmAiEvaluation.Confidence.MEDIUM,
                        java.util.List.of(new SwarmAiEvaluation.Finding(
                                "SEARCH_SUCCESS_RATE", "IMPROVING", "The second run completed all five recorded search episodes."
                        )),
                        java.util.List.of(new SwarmAiEvaluation.ExperimentSuggestion(
                                "BASELINE", "Repeat the baseline with the same agent count."
                        )),
                        java.util.List.of("Only two runs are available.")
                ),
                "llama3.2",
                3600L
        );

        CompoundTag saved = memory.save(new CompoundTag(), null);
        SwarmAiLearningMemory loaded = SwarmAiLearningMemory.load(saved, null);

        assertEquals(2, loaded.runCount());
        assertEquals("llama3.2", loaded.lastEvaluation().model());
        assertEquals(SwarmAiEvaluation.Confidence.MEDIUM,
                loaded.lastEvaluation().evaluation().confidence());
        assertEquals(1, loaded.lastEvaluation().evaluation().findings().size());
    }

    @Test
    void rejectsTooShortOrEmptyExperimentSnapshots() {
        SwarmAiLearningMemory memory = new SwarmAiLearningMemory();
        assertThrows(IllegalStateException.class,
                () -> memory.record(snapshot(199L, 5, 10L, 1L, 2L, 0L, 1L, 0L, 10L, 1L, 1L),
                        "BASELINE", 1, 199L));
        assertThrows(IllegalStateException.class,
                () -> memory.record(snapshot(300L, 0, 10L, 1L, 2L, 0L, 1L, 0L, 10L, 1L, 1L),
                        "BASELINE", 1, 300L));
    }

    private static SwarmExperimentMetrics.Snapshot snapshot(
            long elapsedTicks,
            int agents,
            long delivered,
            long dropped,
            long recoveryAttempts,
            long recoveryFailures,
            long searchSucceeded,
            long searchFailed,
            long reacquisitionTicks,
            long engineeringClaims,
            long engineeringCompleted
    ) {
        return new SwarmExperimentMetrics.Snapshot(
                true, elapsedTicks, agents,
                delivered + dropped, delivered, dropped,
                3L, 2L, recoveryAttempts, recoveryFailures,
                12L, 1L, searchSucceeded + searchFailed, searchSucceeded, searchFailed,
                reacquisitionTicks, 0, 2L, 1L, 0,
                5L, engineeringClaims, engineeringCompleted, 1L, 1L
        );
    }
}
