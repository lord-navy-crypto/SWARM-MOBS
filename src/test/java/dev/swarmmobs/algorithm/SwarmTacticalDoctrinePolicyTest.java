package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmAgentArchetype;
import dev.swarmmobs.agent.SwarmLocalComposition;
import dev.swarmmobs.algorithm.SwarmTacticalDoctrinePolicy.Doctrine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmTacticalDoctrinePolicyTest {
    @Test
    void fullMixedSquadUsesCombinedArmsSpacing() {
        var composition = new SwarmLocalComposition(2, 1, 1, 1);
        var decision = SwarmTacticalDoctrinePolicy.choose(composition, 5, false, 0.9, true);
        assertEquals(Doctrine.FULL_COMBINED_ARMS, decision.doctrine());
        assertTrue(SwarmTacticalDoctrinePolicy.formationRadiusMultiplier(
                decision, SwarmAgentArchetype.FLANKER)
                > SwarmTacticalDoctrinePolicy.formationRadiusMultiplier(
                        decision, SwarmAgentArchetype.ASSAULT));
        assertTrue(SwarmTacticalDoctrinePolicy.formationRadiusMultiplier(
                decision, SwarmAgentArchetype.RANGED_SUPPORT) > 1.0);
    }

    @Test
    void creeperAndSkeletonCompositionUsesBreachSupportDoctrine() {
        var composition = new SwarmLocalComposition(2, 1, 0, 1);
        var decision = SwarmTacticalDoctrinePolicy.choose(composition, 4, false, 0.8, true);
        assertEquals(Doctrine.BREACH_SUPPORT, decision.doctrine());
        assertTrue(SwarmTacticalDoctrinePolicy.formationRadiusMultiplier(
                decision, SwarmAgentArchetype.RANGED_SUPPORT) > 1.0);
        assertTrue(SwarmTacticalDoctrinePolicy.formationRadiusMultiplier(
                decision, SwarmAgentArchetype.BREACHER) < 1.0);
    }

    @Test
    void assaultAndSpiderCompositionPrefersWideFlankGeometry() {
        var composition = new SwarmLocalComposition(3, 0, 2, 0);
        var decision = SwarmTacticalDoctrinePolicy.choose(composition, 5, false, 0.75, true);
        assertEquals(Doctrine.PINCER, decision.doctrine());
        assertTrue(SwarmTacticalDoctrinePolicy.formationRadiusMultiplier(
                decision, SwarmAgentArchetype.FLANKER) > 1.0);
    }

    @Test
    void lostTargetUsesSearchGrid() {
        var composition = new SwarmLocalComposition(3, 1, 1, 1);
        var decision = SwarmTacticalDoctrinePolicy.choose(composition, 6, true, 0.2, true);
        assertEquals(Doctrine.SEARCH_GRID, decision.doctrine());
        assertEquals(1.0, SwarmTacticalDoctrinePolicy.formationRadiusMultiplier(
                decision, SwarmAgentArchetype.FLANKER), 1.0e-9);
    }

    @Test
    void soloOrUncertainGroupsFallBackToStandard() {
        var decision = SwarmTacticalDoctrinePolicy.choose(
                new SwarmLocalComposition(1, 0, 0, 0), 1, false, 0.4, true);
        assertEquals(Doctrine.STANDARD, decision.doctrine());
        assertEquals(Doctrine.STANDARD, SwarmTacticalDoctrinePolicy.choose(
                new SwarmLocalComposition(2, 1, 1, 1), 5, false, 0.9, false).doctrine());
    }
}