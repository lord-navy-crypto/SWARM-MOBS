package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmLocalComposition;
import dev.swarmmobs.agent.SwarmRole;
import dev.swarmmobs.algorithm.SwarmAdaptiveTacticsPolicy.Frame;
import dev.swarmmobs.algorithm.SwarmAdaptiveTacticsPolicy.Pattern;
import dev.swarmmobs.algorithm.SwarmCombatPlanner.Vec2;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmSpiderTacticsPolicyTest {
    @Test void pairedSpidersWidenPincerWhenAssaultFrontlineExists() {
        var composition = new SwarmLocalComposition(2, 0, 2, 0);
        var operation = SwarmCombinedArmsPolicy.choose(composition, 4, false, 0.9);
        var decision = SwarmSpiderTacticsPolicy.choose(composition, operation, false, 0.9);
        assertEquals(SwarmSpiderTacticsPolicy.Mode.PAIRED_PINCER, decision.mode());
        assertTrue(decision.formationRadiusMultiplier() > 1.0);
    }

    @Test void surroundPatternAddsShortArcLeadToSpiderFlanks() {
        var frame = new Frame(Pattern.SURROUND, new Vec2(1, 0), 0);
        Vec2 original = new Vec2(0, 3);
        Vec2 refined = SwarmSpiderTacticsPolicy.refineDestination(
                frame, original, SwarmRole.FLANK_LEFT, 4.0);
        assertTrue(refined.x() > original.x());
        assertEquals(original.z(), refined.z(), 1.0e-9);
    }

    @Test void nonSpiderRolesAndOtherPatternsRemainUnchanged() {
        var frame = new Frame(Pattern.SWEEP, new Vec2(1, 0), 2);
        Vec2 original = new Vec2(0, 3);
        assertEquals(original, SwarmSpiderTacticsPolicy.refineDestination(
                frame, original, SwarmRole.FLANK_LEFT, 4.0));
        assertEquals(original, SwarmSpiderTacticsPolicy.refineDestination(
                new Frame(Pattern.SURROUND, new Vec2(1, 0), 0),
                original, SwarmRole.CHASER, 4.0));
    }
}
