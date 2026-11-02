package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmLocalComposition;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmCombinedArmsPolicyTest {
    @Test void fullMixedGroupGetsAllFourCoordinationChannels() {
        var plan = SwarmCombinedArmsPolicy.choose(
                new SwarmLocalComposition(2, 1, 1, 1), 5, false, 0.9);
        assertEquals(SwarmCombinedArmsPolicy.Operation.FULL_COMBINED_ARMS, plan.operation());
        assertTrue(plan.zombieScreen());
        assertTrue(plan.skeletonOverwatch());
        assertTrue(plan.spiderPincer());
        assertTrue(plan.creeperStaging());
    }

    @Test void creeperSkeletonZombieGroupCreatesBreachScreen() {
        var plan = SwarmCombinedArmsPolicy.choose(
                new SwarmLocalComposition(2, 1, 0, 1), 4, false, 0.8);
        assertEquals(SwarmCombinedArmsPolicy.Operation.BREACH_SCREEN, plan.operation());
        assertTrue(plan.zombieScreen());
        assertTrue(plan.skeletonOverwatch());
        assertTrue(plan.creeperStaging());
        assertFalse(plan.spiderPincer());
    }

    @Test void targetLossSwitchesToSearchRelayWithoutCombatStaging() {
        var plan = SwarmCombinedArmsPolicy.choose(
                new SwarmLocalComposition(2, 1, 1, 1), 5, true, 0.2);
        assertEquals(SwarmCombinedArmsPolicy.Operation.SEARCH_RELAY, plan.operation());
        assertFalse(plan.creeperStaging());
    }

    @Test void weakConfidenceDoesNotInventCombinedArmsPlan() {
        var plan = SwarmCombinedArmsPolicy.choose(
                new SwarmLocalComposition(2, 1, 1, 1), 5, false, 0.4);
        assertEquals(SwarmCombinedArmsPolicy.Operation.INDEPENDENT, plan.operation());
    }
}
