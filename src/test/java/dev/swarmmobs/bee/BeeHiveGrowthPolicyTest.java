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
    @Test void vanillaHiveGrowthRequiresOccupiedHomeAndFullHoney() {
        assertTrue(BeeHiveGrowthPolicy.canGrowVanilla(3, 3, 5, 5, true, true));
        assertFalse(BeeHiveGrowthPolicy.canGrowVanilla(2, 3, 5, 5, true, true));
        assertFalse(BeeHiveGrowthPolicy.canGrowVanilla(3, 3, 4, 5, true, true));
        assertFalse(BeeHiveGrowthPolicy.canGrowVanilla(3, 3, 5, 5, false, true));
        assertFalse(BeeHiveGrowthPolicy.canGrowVanilla(3, 3, 5, 5, true, false));
    }

    @Test void vanillaHiveGrowthDoesNotEnforceLifetimeStructureLimit() {
        for (int oldExtensions : new int[]{0, 8, 1000, 100_000}) {
            // Vanilla rule intentionally has no total-extension argument.
            assertTrue(BeeHiveGrowthPolicy.canGrowVanilla(3, 3, 5, 5, true, true));
        }
    }

}
