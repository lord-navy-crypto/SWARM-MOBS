package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmAgentArchetype;
import dev.swarmmobs.algorithm.SwarmCombatPlanner.Vec2;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmSearchCoveragePolicyTest {
    private static SwarmSearchPlanner.SearchPlan plan() {
        return SwarmSearchPlanner.planWithMotion(
                SwarmAgentArchetype.FLANKER, 0,
                new Vec2(0, 0), new Vec2(0, 0), new Vec2(10, 10),
                0.2, 80, java.util.List.of(), java.util.List.of(),
                4, 2, 10, 20, 2.4, 0, 0, 0, 3);
    }

    @Test
    void repeatedSectorMovesToAnUnderVisitedNeighboringSector() {
        var original = plan();
        Vec2 center = new Vec2(10, 10);
        Vec2 originalAnchor = center.add(new Vec2(
                Math.cos(original.angleRadians()), Math.sin(original.angleRadians()))
                .scale(original.searchRadius()));
        var adjustment = SwarmSearchCoveragePolicy.adjust(
                original, center, 0,
                candidate -> candidate.subtract(originalAnchor).length() < 0.01 ? 2.0 : 0.0,
                true);
        assertNotEquals(0, adjustment.sectorShift());
        assertTrue(adjustment.selectedCoverage() < adjustment.originalCoverage());
        assertNotEquals(original.destination(), adjustment.plan().destination());
    }

    @Test
    void unvisitedSectorsPreserveTheStableOriginalPlan() {
        var original = plan();
        var adjustment = SwarmSearchCoveragePolicy.adjust(
                original, new Vec2(10, 10), 3, candidate -> 0.0, true);
        assertEquals(0, adjustment.sectorShift());
        assertEquals(original, adjustment.plan());
    }

    @Test
    void disabledOrInvalidCoverageNeverChangesThePlan() {
        var original = plan();
        assertEquals(original, SwarmSearchCoveragePolicy.adjust(
                original, new Vec2(10, 10), 0, candidate -> 5.0, false).plan());
        assertEquals(original, SwarmSearchCoveragePolicy.adjust(
                original, new Vec2(Double.NaN, 10), 0, candidate -> 5.0, true).plan());
    }
}
