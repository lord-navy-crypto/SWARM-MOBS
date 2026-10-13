package dev.swarmmobs.bee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeHiveGrowthPolicyTest {
    @Test void fullHiveWithSurplusAndWaxMayGrow() {
        assertTrue(BeeHiveGrowthPolicy.canExpand(
                new BeeHiveGrowthPolicy.State(3, 3, 70, 30, 12, 10, 2, 0, 6)));
    }

    @Test void conserveHoneyNeededForSurvival() {
        assertFalse(BeeHiveGrowthPolicy.canExpand(
                new BeeHiveGrowthPolicy.State(3, 3, 30, 30, 12, 10, 2, 0, 6)));
    }

    @Test void expansionIsBoundedByFreeSpaceAndLimit() {
        assertFalse(BeeHiveGrowthPolicy.canExpand(
                new BeeHiveGrowthPolicy.State(3, 3, 70, 30, 12, 10, 0, 0, 6)));
        assertFalse(BeeHiveGrowthPolicy.canExpand(
                new BeeHiveGrowthPolicy.State(3, 3, 70, 30, 12, 10, 2, 6, 6)));
    }
}
