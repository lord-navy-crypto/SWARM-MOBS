package dev.swarmmobs.colony;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MonsterTrailPolicyTest {
    @Test void evidenceHalvesAtHalfLife() {
        assertEquals(0.5, MonsterTrailPolicy.decay(1, 100, 100), 1e-12);
    }

    @Test void reinforcingDoesNotExceedCapacity() {
        assertTrue(MonsterTrailPolicy.reinforce(0.95, 1, 1) <= 1);
    }

    @Test void dangerCanOutweighTrailStrength() {
        assertTrue(MonsterTrailPolicy.routeUtility(0.7, 0, 1, 0) <
                   MonsterTrailPolicy.routeUtility(0.4, 0, 0, 0));
    }
}
