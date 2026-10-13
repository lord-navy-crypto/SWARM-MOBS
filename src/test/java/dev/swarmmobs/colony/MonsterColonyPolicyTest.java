package dev.swarmmobs.colony;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MonsterColonyPolicyTest {
    private static final MonsterColonyPolicy.Capabilities SOLDIER =
            new MonsterColonyPolicy.Capabilities(1, 0.1, 0, 0.4);
    private static final MonsterColonyPolicy.Capabilities BUILDER =
            new MonsterColonyPolicy.Capabilities(0.1, 0.5, 1, 0.2);

    @Test void soldierDefendsWhenNestThreatened() {
        var input = new MonsterColonyPolicy.Signals(1, 0.3, 0.4, 0.5, 0);
        assertEquals(MonsterColonyPolicy.Mission.DEFEND,
                MonsterColonyPolicy.select(input, SOLDIER,
                        MonsterColonyPolicy.Mission.PATROL, 0.1));
    }

    @Test void builderRepairsStructuralDamage() {
        var input = new MonsterColonyPolicy.Signals(0, 0.2, 1, 0.1, 0);
        assertEquals(MonsterColonyPolicy.Mission.REPAIR,
                MonsterColonyPolicy.select(input, BUILDER,
                        MonsterColonyPolicy.Mission.PATROL, 0.1));
    }

    @Test void boundedHysteresisPreventsUnnecessaryChurn() {
        var input = new MonsterColonyPolicy.Signals(0.4, 0, 0, 0, 0);
        assertEquals(MonsterColonyPolicy.Mission.PATROL,
                MonsterColonyPolicy.select(input, SOLDIER,
                        MonsterColonyPolicy.Mission.PATROL, 0.6));
    }

    @Test void badSignalsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new MonsterColonyPolicy.Signals(Double.NaN, 0, 0, 0, 0));
    }
}
