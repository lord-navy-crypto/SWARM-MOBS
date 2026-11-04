package dev.swarmmobs.bee;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeTaskMemoryTest {
    private static final BeeTaskPolicy.Needs EMPTY = new BeeTaskPolicy.Needs(0, 0, 0, 0, 0);

    @Test
    void currentTaskLookupRequiresFreshAssignmentInTheSameHive() {
        var memory = new BeeTaskMemory();
        UUID bee = new UUID(0, 18);
        var threat = new BeeTaskPolicy.Needs(0, 0, 1, 0, 0);
        assertNull(memory.task(bee, 10L, 100L));

        assertEquals(BeeTaskPolicy.Task.GUARD, memory.assign(bee, 10L, threat, 100L));
        assertEquals(BeeTaskPolicy.Task.GUARD, memory.task(bee, 10L, 200L));
        assertNull(memory.task(bee, 11L, 200L));
        assertNull(memory.task(bee, 10L, 501L));
    }

    @Test
    void workforceCountsAreScopedToTheAssignedHive() {
        var memory = new BeeTaskMemory();
        long hiveA = 101L;
        long hiveB = 202L;
        var threat = new BeeTaskPolicy.Needs(0, 0, 1, 0, 0);

        assertEquals(BeeTaskPolicy.Task.GUARD,
                memory.assign(new UUID(0, 8), hiveA, threat, 100));
        assertEquals(BeeTaskPolicy.Task.GUARD,
                memory.assign(new UUID(0, 10), hiveA, threat, 100));

        var sameHive = memory.workforce(hiveA, 101, new UUID(0, 11));
        var otherHive = memory.workforce(hiveB, 101, new UUID(0, 11));
        assertEquals(2, sameHive.guardWorkers());
        assertEquals(3, sameHive.estimatedPopulation());
        assertEquals(0, otherHive.guardWorkers());
        assertEquals(1, otherHive.estimatedPopulation());
    }

    @Test
    void taskCensusExpiresBeforeLongerLivedHysteresisMemory() {
        var memory = new BeeTaskMemory();
        long hive = 303L;
        var threat = new BeeTaskPolicy.Needs(0, 0, 1, 0, 0);
        memory.assign(new UUID(0, 8), hive, threat, 100);

        assertEquals(1, memory.workforce(hive, 501, new UUID(0, 12)).estimatedPopulation());
        assertEquals(1, memory.size(501));
    }

    @Test
    void movingBetweenHivesResetsPreviousColonyTask() {
        var memory = new BeeTaskMemory();
        UUID bee = new UUID(0, 8);
        var threat = new BeeTaskPolicy.Needs(0, 0, 1, 0, 0);
        assertEquals(BeeTaskPolicy.Task.GUARD, memory.assign(bee, 10L, threat, 100));
        assertEquals(BeeTaskPolicy.Task.FORAGE,
                memory.assign(bee, 20L, EMPTY, 101));
    }

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
