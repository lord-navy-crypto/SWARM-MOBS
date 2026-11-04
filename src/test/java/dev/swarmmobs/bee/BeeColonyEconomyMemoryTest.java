package dev.swarmmobs.bee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeColonyEconomyMemoryTest {
    @Test void birthsAreLimitedPerConnectedColony() {
        var memory = new BeeColonyEconomyMemory();
        assertFalse(memory.claimBirth(10, 100, 600));
        assertFalse(memory.claimBirth(10, 699, 600));
        assertTrue(memory.claimBirth(10, 700, 600));
        assertFalse(memory.claimBirth(10, 701, 600));
        assertFalse(memory.claimBirth(11, 701, 600));
    }

    @Test void honeyCreditsHaveIndependentPerHiveCooldowns() {
        var memory = new BeeColonyEconomyMemory();
        assertFalse(memory.claimHoney(20, 100, 300));
        assertTrue(memory.claimHoney(20, 400, 300));
        assertFalse(memory.claimHoney(20, 401, 300));
        assertFalse(memory.claimHoney(21, 401, 300));
    }

    @Test void memoryIsBoundedAndTimeRewindStartsANewLease() {
        var memory = new BeeColonyEconomyMemory();
        assertFalse(memory.claimBirth(1, 1000, 200));
        assertFalse(memory.claimBirth(1, 900, 200));
        for (int i = 0; i < BeeColonyEconomyMemory.MAX_TRACKED_HIVES + 10; i++)
            memory.claimBirth(i, 20000L + i, 24000);
        assertTrue(memory.trackedColonies(30000)
                <= BeeColonyEconomyMemory.MAX_TRACKED_HIVES);
    }
}
