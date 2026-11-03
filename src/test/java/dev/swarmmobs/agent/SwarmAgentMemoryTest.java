package dev.swarmmobs.agent;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SwarmAgentMemoryTest {
    @Test
    void taskExperienceSurvivesEntityNbtRoundTrip() {
        SwarmAgentState original = new SwarmAgentState();
        original.updateTaskExperience(SwarmTaskType.ENGINEERING, 0.25, 1.0, 100L);
        original.updateTaskExperience(SwarmTaskType.ENGINEERING, 0.25, 1.0, 106L);
        original.updateTaskExperience(SwarmTaskType.SEARCH, 0.15, 1.0, 112L);

        CompoundTag entityData = new CompoundTag();
        SwarmAgentMemory.save(entityData, original, "minecraft:overworld", 112L);

        SwarmAgentState restored = new SwarmAgentState();
        assertTrue(SwarmAgentMemory.restore(entityData, restored, 50_000L));
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

        SwarmAgentState restored = new SwarmAgentState();
        restored.restoreTaskExperience(Map.of(SwarmTaskType.MATERIAL, learned), 100_000L);
        assertEquals(learned, restored.taskExperience(SwarmTaskType.MATERIAL), 1.0e-9);

        restored.updateTaskExperience(null, 0.3, 0.95, 100_001L);
        assertTrue(restored.taskExperience(SwarmTaskType.MATERIAL) < learned);
        assertTrue(restored.taskExperience(SwarmTaskType.MATERIAL) > 0.0);
    }

    @Test
    void invalidMemorySchemaIsIgnored() {
        CompoundTag entityData = new CompoundTag();
        CompoundTag memory = new CompoundTag();
        memory.putInt("schemaVersion", SwarmAgentMemory.SCHEMA_VERSION + 1);
        memory.put("taskExperience", new CompoundTag());
        entityData.put(SwarmAgentMemory.DATA_KEY, memory);

        assertFalse(SwarmAgentMemory.restore(entityData, new SwarmAgentState(), 500L));
    }

    @Test
    void periodicWritesAreDeterministicallySpread() {
        assertTrue(SwarmAgentMemory.shouldPersist(17L, 17));
        assertFalse(SwarmAgentMemory.shouldPersist(18L, 17));
        assertTrue(SwarmAgentMemory.shouldPersist(217L, 17));
    }
}
