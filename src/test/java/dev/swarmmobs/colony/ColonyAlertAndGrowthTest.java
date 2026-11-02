package dev.swarmmobs.colony;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColonyAlertAndGrowthTest {
    @Test void playerSightingOverridesEveryJob() {
        var o = new ColonyAlertPolicy.Observation(true, false, false, true, true, 0);
        for (var prior : ColonyAlertPolicy.Mode.values()) {
            assertEquals(ColonyAlertPolicy.Mode.COMBAT, ColonyAlertPolicy.next(prior, o, 100));
        }
        assertTrue(ColonyAlertPolicy.suspendsColonyJobs(ColonyAlertPolicy.Mode.COMBAT));
    }

    @Test void alertsMobilizeAndMemoryGuidesSearch() {
        var message = new ColonyAlertPolicy.Observation(false, false, true, false, true, 0);
        assertEquals(ColonyAlertPolicy.Mode.MOBILIZING,
                ColonyAlertPolicy.next(ColonyAlertPolicy.Mode.WORK, message, 100));
        var quiet = new ColonyAlertPolicy.Observation(false, false, false, false, true, 0);
        assertEquals(ColonyAlertPolicy.Mode.SEARCH,
                ColonyAlertPolicy.next(ColonyAlertPolicy.Mode.COMBAT, quiet, 100));
    }

    @Test void completedAlertReturnsToWorkAfterCooldown() {
        var quiet = new ColonyAlertPolicy.Observation(false, false, false, false, false, 110);
        assertEquals(ColonyAlertPolicy.Mode.WORK,
                ColonyAlertPolicy.next(ColonyAlertPolicy.Mode.RECOVER, quiet, 100));
    }

    @Test void monsterGrowthRequiresFullNestAndResources() {
        var full = new ColonyGrowthPolicy.State(8, 8, 100, 100, 2, 1, 5);
        assertTrue(ColonyGrowthPolicy.canExpand(full, 30));
        assertFalse(ColonyGrowthPolicy.canExpand(
                new ColonyGrowthPolicy.State(7, 8, 100, 100, 2, 1, 5), 30));
        assertFalse(ColonyGrowthPolicy.canExpand(
                new ColonyGrowthPolicy.State(8, 8, 90, 100, 2, 1, 5), 30));
        assertFalse(ColonyGrowthPolicy.canExpand(
                new ColonyGrowthPolicy.State(8, 8, 100, 100, 0, 1, 5), 30));
    }
}
