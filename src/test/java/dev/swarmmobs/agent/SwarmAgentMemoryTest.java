package dev.swarmmobs.agent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SwarmAgentMemoryTest {
    @Test
    void taskExperienceSurvivesMemoryPolicyRoundTrip() {
        SwarmAgentState original = new SwarmAgentState();
        original.updateTaskExperience(SwarmTaskType.ENGINEERING, 0.25, 1.0, 100L);
        original.updateTaskExperience(SwarmTaskType.ENGINEERING, 0.25, 1.0, 106L);
        original.updateTaskExperience(SwarmTaskType.SEARCH, 0.15, 1.0, 112L);

        Map<String, Double> encoded =
                SwarmAgentMemoryPolicy.encode(original.taskExperienceSnapshot());
        Map<SwarmTaskType, Double> decoded = SwarmAgentMemoryPolicy.decode(
                SwarmAgentMemoryPolicy.SCHEMA_VERSION, encoded);

        SwarmAgentState restored = new SwarmAgentState();
        restored.restoreTaskExperience(decoded, 50_000L);
        assertEquals(original.taskExperience(SwarmTaskType.ENGINEERING),
                restored.taskExperience(SwarmTaskType.ENGINEERING), 1.0e-9);
        assertEquals(original.taskExperience(SwarmTaskType.SEARCH),
                restored.taskExperience(SwarmTaskType.SEARCH), 1.0e-9);
        assertEquals(0.0, restored.taskExperience(SwarmTaskType.FLANK), 1.0e-9);
    }

    @Test
    void restoringMemoryDoesNotApplyOfflineDecay() {
        SwarmAgentState original = new SwarmAgentState();
        original.updateTaskExperience(SwarmTaskType.MATERIAL, 0.3, 0.95, 100L);
        original.updateTaskExperience(SwarmTaskType.MATERIAL, 0.3, 0.95, 106L);
        double learned = original.taskExperience(SwarmTaskType.MATERIAL);

        Map<SwarmTaskType, Double> decoded = SwarmAgentMemoryPolicy.decode(
                SwarmAgentMemoryPolicy.SCHEMA_VERSION,
                SwarmAgentMemoryPolicy.encode(original.taskExperienceSnapshot()));
        SwarmAgentState restored = new SwarmAgentState();
        restored.restoreTaskExperience(decoded, 100_000L);
        assertEquals(learned, restored.taskExperience(SwarmTaskType.MATERIAL), 1.0e-9);

        restored.updateTaskExperience(null, 0.3, 0.95, 100_001L);
        assertTrue(restored.taskExperience(SwarmTaskType.MATERIAL) < learned);
        assertTrue(restored.taskExperience(SwarmTaskType.MATERIAL) > 0.0);
    }

    @Test
    void invalidMemorySchemaIsIgnored() {
        assertTrue(SwarmAgentMemoryPolicy.decode(
                SwarmAgentMemoryPolicy.SCHEMA_VERSION + 1,
                Map.of("ENGINEERING", 0.8)).isEmpty());
    }

    @Test
    void memoryValuesAreClampedAndUnknownTasksAreIgnored() {
        Map<SwarmTaskType, Double> decoded = SwarmAgentMemoryPolicy.decode(
                SwarmAgentMemoryPolicy.SCHEMA_VERSION,
                Map.of("ENGINEERING", 2.0, "FLANK", -1.0, "UNKNOWN", 0.7));
        assertEquals(1.0, decoded.get(SwarmTaskType.ENGINEERING));
        assertEquals(0.0, decoded.get(SwarmTaskType.FLANK));
        assertFalse(decoded.containsKey(SwarmTaskType.SEARCH));
    }

    @Test
    void periodicWritesAreDeterministicallySpread() {
        assertTrue(SwarmAgentMemoryPolicy.shouldPersist(17L, 17));
        assertFalse(SwarmAgentMemoryPolicy.shouldPersist(18L, 17));
        assertTrue(SwarmAgentMemoryPolicy.shouldPersist(217L, 17));
    }
}
