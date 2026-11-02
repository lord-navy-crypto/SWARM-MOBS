package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmLocalComposition;
import dev.swarmmobs.algorithm.SwarmCombatPlanner.Vec2;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmCreeperTacticsPolicyTest {
    @Test void creeperStagesOnlyWhenSupportedCombinedArmsWindowExists() {
        var plan = SwarmCombinedArmsPolicy.choose(
                new SwarmLocalComposition(2, 1, 0, 1), 4, false, 0.9);
        assertEquals(SwarmCreeperTacticsPolicy.Mode.STAGED_INGRESS,
                SwarmCreeperTacticsPolicy.choose(plan, false, true, 0.9, 10, false).mode());
    }

    @Test void fuseAlwaysReturnsControlToVanillaBehavior() {
        var plan = SwarmCombinedArmsPolicy.choose(
                new SwarmLocalComposition(2, 1, 0, 1), 4, false, 0.9);
        assertEquals(SwarmCreeperTacticsPolicy.Mode.VANILLA_FUSE,
                SwarmCreeperTacticsPolicy.choose(plan, false, true, 0.9, 10, true).mode());
    }

    @Test void weakOrIndirectTargetCannotTriggerStagedIngress() {
        var plan = SwarmCombinedArmsPolicy.choose(
                new SwarmLocalComposition(2, 1, 0, 1), 4, false, 0.9);
        assertEquals(SwarmCreeperTacticsPolicy.Mode.APPROACH,
                SwarmCreeperTacticsPolicy.choose(plan, false, false, 0.9, 10, false).mode());
        assertEquals(SwarmCreeperTacticsPolicy.Mode.APPROACH,
                SwarmCreeperTacticsPolicy.choose(plan, false, true, 0.4, 10, false).mode());
    }

    @Test void stagingWaypointUsesLowerAllyOccupancyAndIsBounded() {
        Vec2 self = new Vec2(0, 0), target = new Vec2(10, 0);
        Vec2 axis = new Vec2(1, 0);
        Vec2 left = new Vec2(5.5, 2.0);
        var choice = SwarmCreeperTacticsPolicy.stagingWaypoint(
                self, target, axis, List.of(left), 0);
        assertNotNull(choice);
        assertTrue(choice.subtract(self).length() < 12.0);
        assertTrue(choice.subtract(target).length() >=
                SwarmCreeperTacticsPolicy.STAGING_STANDOFF);
    }

    @Test void reachingStagingSquareReleasesWaypointForVanillaApproach() {
        Vec2 self = new Vec2(5.5, 2.0), target = new Vec2(10, 0);
        assertNull(SwarmCreeperTacticsPolicy.stagingWaypoint(
                self, target, new Vec2(1, 0), List.of(), 0));
    }
}
