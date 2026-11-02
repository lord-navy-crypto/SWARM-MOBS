package dev.swarmmobs.bee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeEcologyPolicyTest {
    @Test void configuredHiveCapacityIsBounded() {
        assertEquals(3, BeeEcologyPolicy.hiveCapacity(-10));
        assertEquals(8, BeeEcologyPolicy.hiveCapacity(8));
        assertEquals(16, BeeEcologyPolicy.hiveCapacity(99));
    }

    @Test void babyMaturationAndAdultBreedingCooldownAccelerateTowardZero() {
        assertEquals(-8, BeeEcologyPolicy.acceleratedAge(-10, 3));
        assertEquals(8, BeeEcologyPolicy.acceleratedAge(10, 3));
        assertEquals(0, BeeEcologyPolicy.acceleratedAge(1, 3));
        assertEquals(0, BeeEcologyPolicy.acceleratedAge(0, 3));
        assertThrows(IllegalArgumentException.class,
                () -> BeeEcologyPolicy.acceleratedAge(-10, 9));
    }

    @Test void honeyBonusScalesWithStoredWorkersAndConfiguredMultiplier() {
        assertEquals(2400, BeeEcologyPolicy.honeyBonusInterval(4800, 2, 3));
        assertEquals(800, BeeEcologyPolicy.honeyBonusInterval(4800, 2, 8));
        assertEquals(100, BeeEcologyPolicy.honeyBonusInterval(4800, 5, 128));
    }

    @Test void recruitmentRequiresPopulationRoomHoneyFlowersAndSpawnRule() {
        assertTrue(BeeEcologyPolicy.shouldRecruit(7, 48, 8, 3, 2, true, true));
        assertFalse(BeeEcologyPolicy.shouldRecruit(48, 48, 8, 3, 2, true, true));
        assertFalse(BeeEcologyPolicy.shouldRecruit(7, 48, 3, 3, 2, true, true));
        assertFalse(BeeEcologyPolicy.shouldRecruit(7, 48, 8, 3, 0, true, true));
        assertFalse(BeeEcologyPolicy.shouldRecruit(7, 48, 8, 3, 2, true, false));
    }
}
