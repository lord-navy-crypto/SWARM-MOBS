package dev.swarmmobs.bee;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeTaskMemoryTest {
    private static final UUID BEE = new UUID(0, 42);

    @Test void repeatedAssignmentsAreStableWhenNeedsDoNotChange() {
        var memory = new BeeTaskMemory();
        var needs = new BeeTaskPolicy.Needs(0.9, 0.0, 0.0, 0.2, 0.0);
        var first = memory.assign(BEE, needs, 100);
        var second = memory.assign(BEE, needs, 200);
        assertEquals(first, second);
        assertEquals(1, memory.size(200));
    }

    @Test void strongObservedThreatCanReassignWorkerToGuardPriority() {
        var memory = new BeeTaskMemory();
        memory.assign(BEE, new BeeTaskPolicy.Needs(0.2, 0, 0, 0.1, 0), 100);
        var guarded = memory.assign(BEE,
                new BeeTaskPolicy.Needs(0.1, 0, 1.0, 0.1, 0), 200);
        assertEquals(BeeTaskPolicy.Task.GUARD, guarded);
    }

    @Test void assignmentsExpireAndCacheRemainsBounded() {
        var memory = new BeeTaskMemory();
        memory.assign(BEE, new BeeTaskPolicy.Needs(0.5, 0, 0, 0, 0), 100);
        assertEquals(0, memory.size(100 + BeeTaskMemory.MEMORY_TICKS));
        for (int i = 0; i < BeeTaskMemory.MAX_ASSIGNMENTS + 20; i++) {
            memory.assign(new UUID(1, i),
                    new BeeTaskPolicy.Needs(0.5, 0, 0, 0, 0), 20000 + i);
        }
        assertTrue(memory.size(30000) <= BeeTaskMemory.MAX_ASSIGNMENTS);
    }

    @Test void clearAllowsWorkerToReevaluateAfterHiveExpansion() {
        var memory = new BeeTaskMemory();
        var buildNeeds = new BeeTaskPolicy.Needs(0.0, 0, 0, 1.0, 0);
        assertEquals(BeeTaskPolicy.Task.BUILD, memory.assign(BEE, buildNeeds, 100));
        memory.clear(BEE);
        var forageNeeds = new BeeTaskPolicy.Needs(1.0, 0, 0, 0, 0);
        assertEquals(BeeTaskPolicy.Task.FORAGE, memory.assign(BEE, forageNeeds, 101));
    }

    @Test void invalidAssignmentInputsAreRejected() {
        var memory = new BeeTaskMemory();
        assertThrows(IllegalArgumentException.class, () ->
                memory.assign(null, new BeeTaskPolicy.Needs(0, 0, 0, 0, 0), 1));
        assertThrows(IllegalArgumentException.class, () ->
                memory.assign(BEE, new BeeTaskPolicy.Needs(0, 0, 0, 0, 0), -1));
    }
}
