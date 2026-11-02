package dev.swarmmobs.bee;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeTaskMemoryTest {
    private static final BeeTaskPolicy.Needs EMPTY = new BeeTaskPolicy.Needs(0, 0, 0, 0, 0);

    @Test void staleBuildAssignmentReturnsToForagingWhenExpansionIsNoLongerReady() {
        var memory = new BeeTaskMemory();
        UUID bee = new UUID(0, 7);
        var buildNeed = new BeeTaskPolicy.Needs(0, 0, 0, 1, 0);
        assertEquals(BeeTaskPolicy.Task.BUILD, memory.assign(bee, buildNeed, 100));
        assertEquals(BeeTaskPolicy.Task.FORAGE, memory.assign(bee, EMPTY, 101));
    }

    @Test void staleGuardAndNurseAssignmentsDoNotPersistWithoutTheirNeeds() {
        var memory = new BeeTaskMemory();
        UUID guard = new UUID(0, 8);
        var threat = new BeeTaskPolicy.Needs(0, 0, 1, 0, 0);
        assertEquals(BeeTaskPolicy.Task.GUARD, memory.assign(guard, threat, 100));
        assertEquals(BeeTaskPolicy.Task.FORAGE, memory.assign(guard, EMPTY, 101));

        UUID nurse = new UUID(0, 9);
        var brood = new BeeTaskPolicy.Needs(0, 1, 0, 0, 0);
        assertEquals(BeeTaskPolicy.Task.NURSE, memory.assign(nurse, brood, 100));
        assertEquals(BeeTaskPolicy.Task.FORAGE, memory.assign(nurse, EMPTY, 101));
    }
}
