package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmLocalComposition;
import dev.swarmmobs.agent.SwarmRole;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmZombieTacticsPolicyTest {
    @Test void frontZombieScreensOverwatchWhenSkeletonsArePresent() {
        var plan = SwarmCombinedArmsPolicy.choose(
                new SwarmLocalComposition(2, 1, 1, 0), 4, false, 0.9);
        var decision = SwarmZombieTacticsPolicy.choose(
                SwarmRole.CHASER, new SwarmLocalComposition(2, 1, 1, 0),
                plan, false, 0.9);
        assertEquals(SwarmZombieTacticsPolicy.Mode.FRONT_SCREEN, decision.mode());
        assertTrue(decision.formationRadiusMultiplier() < 1.0);
    }

    @Test void flankZombieCoversTheGapOrSupportsSpiderPincer() {
        var composition = new SwarmLocalComposition(2, 0, 2, 0);
        var plan = SwarmCombinedArmsPolicy.choose(composition, 4, false, 0.9);
        var decision = SwarmZombieTacticsPolicy.choose(
                SwarmRole.FLANK_LEFT, composition, plan, false, 0.9);
        assertEquals(SwarmZombieTacticsPolicy.Mode.SECONDARY_PRESSURE, decision.mode());
        assertTrue(decision.formationRadiusMultiplier() > 1.0);
    }

    @Test void targetLossUsesSearchPosture() {
        var decision = SwarmZombieTacticsPolicy.choose(
                SwarmRole.CHASER, new SwarmLocalComposition(1, 0, 0, 0),
                null, true, 0.2);
        assertEquals(SwarmZombieTacticsPolicy.Mode.SEARCH_SCOUT, decision.mode());
    }
}
