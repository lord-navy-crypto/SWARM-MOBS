package dev.swarmmobs.bee;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeForagingCycleMemoryTest {
    private static final UUID BEE = new UUID(0, 42);
    private static final long HIVE = 100L;
    private static final long FLOWER = 200L;

    @Test void recordsOnlyOneSuccessfulReturnOnNectarTransitionNearHive() {
        var memory = new BeeForagingCycleMemory();
        memory.observe(BEE, false, null, HIVE, true, 100);
        memory.observe(BEE, true, FLOWER, HIVE, false, 200);
        var returned = memory.observe(BEE, false, null, HIVE, true, 450);
        assertTrue(returned.completedTrip());
        assertEquals(FLOWER, returned.completedFlower());
        assertEquals(HIVE, returned.completedHive());
        assertEquals(1, returned.completedTripsSinceRest());
    }

    @Test void doesNotCountLostNectarAwayFromHiveAsSuccessfulTrip() {
        var memory = new BeeForagingCycleMemory();
        memory.observe(BEE, false, null, HIVE, true, 100);
        memory.observe(BEE, true, FLOWER, HIVE, false, 200);
        var lost = memory.observe(BEE, false, null, HIVE, false, 220);
        assertFalse(lost.completedTrip());
        assertEquals(0, memory.completedTrips(BEE, 220));
    }

    @Test void restRequestIsNotExtendedAndRecoversFatigueAfterReentry() {
        var memory = new BeeForagingCycleMemory();
        memory.observe(BEE, false, null, HIVE, true, 100);
        memory.observe(BEE, true, FLOWER, HIVE, false, 200);
        var returned = memory.observe(BEE, false, null, HIVE, true, 450);
        assertTrue(returned.completedTrip());
        assertTrue(memory.markRestRequested(BEE, 451));
        assertFalse(memory.markRestRequested(BEE, 500));
        memory.observe(BEE, false, null, HIVE, false, 452);
        var rested = memory.observe(BEE, false, null, HIVE, true, 700);
        assertEquals(0, rested.completedTripsSinceRest());
        assertTrue(rested.fatigue() < returned.fatigue());
    }

    @Test void ordinaryHiveStayProvidesSmallRecovery() {
        var memory = new BeeForagingCycleMemory();
        memory.observe(BEE, false, null, HIVE, true, 100);
        memory.observe(BEE, true, FLOWER, HIVE, false, 200);
        var first = memory.observe(BEE, false, null, HIVE, true, 450);
        memory.observe(BEE, true, FLOWER, HIVE, false, 600);
        var second = memory.observe(BEE, false, null, HIVE, true, 850);
        assertTrue(second.fatigue() > first.fatigue());
        assertEquals(2, second.completedTripsSinceRest());
    }

    @Test void boundedAndExpiringMemory() {
        var memory = new BeeForagingCycleMemory();
        for (int i = 0; i < BeeForagingCycleMemory.MAX_BEES + 20; i++) {
            memory.observe(new UUID(1, i), false, null, HIVE, true, 100 + i);
        }
        assertTrue(memory.size(100_000) <= BeeForagingCycleMemory.MAX_BEES);
    }

    @Test void invalidObservationsAreRejected() {
        var memory = new BeeForagingCycleMemory();
        assertThrows(IllegalArgumentException.class,
                () -> memory.observe(null, false, null, HIVE, true, 1));
        assertThrows(IllegalArgumentException.class,
                () -> memory.observe(BEE, false, null, HIVE, true, -1));
    }
}
