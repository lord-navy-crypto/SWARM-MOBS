package dev.swarmmobs.bee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeHiveGrowthMemoryTest {
    @Test void onlyOneBeeCanClaimTheSameHiveInOneGameTick() {
        var memory = new BeeHiveGrowthMemory();
        assertTrue(memory.claim(123L, 400L));
        assertFalse(memory.claim(123L, 400L));
        assertTrue(memory.claim(123L, 800L));
    }

    @Test void olderTicksCannotReopenAnAlreadyProcessedGrowthWindow() {
        var memory = new BeeHiveGrowthMemory();
        assertTrue(memory.claim(123L, 800L));
        assertFalse(memory.claim(123L, 799L));
    }

    @Test void memoryExpiresAndRemainsBounded() {
        var memory = new BeeHiveGrowthMemory();
        memory.claim(1L, 100L);
        assertEquals(0, memory.size(100L + BeeHiveGrowthMemory.MEMORY_TICKS));
        for (int i = 0; i < BeeHiveGrowthMemory.MAX_TRACKED_HIVES + 20; i++) {
            assertTrue(memory.claim(i + 10L, 20000L + i));
        }
        assertTrue(memory.size(30000L) <= BeeHiveGrowthMemory.MAX_TRACKED_HIVES);
    }
}
