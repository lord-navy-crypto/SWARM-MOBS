package dev.swarmmobs.algorithm;

import dev.swarmmobs.algorithm.SwarmCombatPlanner.Vec2;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmTacticalHysteresisPolicyTest {
    private static SwarmAdaptiveTacticsPolicy.Frame frame(
            SwarmAdaptiveTacticsPolicy.Pattern pattern, double x) {
        return new SwarmAdaptiveTacticsPolicy.Frame(pattern, new Vec2(x, 0), 1.0);
    }

    @Test void firstFrameIsAcceptedImmediately() {
        var result = SwarmTacticalHysteresisPolicy.update(
                SwarmTacticalHysteresisPolicy.State.empty(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.STANDARD, 1), 10, 12);
        assertTrue(result.switched());
        assertEquals(SwarmAdaptiveTacticsPolicy.Pattern.STANDARD, result.frame().pattern());
    }

    @Test void changedPatternMustRemainStableForTheHoldWindow() {
        var first = SwarmTacticalHysteresisPolicy.update(
                SwarmTacticalHysteresisPolicy.State.empty(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.STANDARD, 1), 10, 12);
        var pending = SwarmTacticalHysteresisPolicy.update(first.state(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.SWEEP, 0), 11, 12);
        assertFalse(pending.switched());
        assertEquals(SwarmAdaptiveTacticsPolicy.Pattern.STANDARD, pending.frame().pattern());
        var stillPending = SwarmTacticalHysteresisPolicy.update(pending.state(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.SWEEP, 0), 22, 12);
        assertFalse(stillPending.switched());
        var accepted = SwarmTacticalHysteresisPolicy.update(stillPending.state(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.SWEEP, 0), 23, 12);
        assertTrue(accepted.switched());
        assertEquals(SwarmAdaptiveTacticsPolicy.Pattern.SWEEP, accepted.frame().pattern());
    }

    @Test void returningToActivePatternCancelsPendingSwitch() {
        var first = SwarmTacticalHysteresisPolicy.update(
                SwarmTacticalHysteresisPolicy.State.empty(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.STANDARD, 1), 10, 12);
        var pending = SwarmTacticalHysteresisPolicy.update(first.state(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.SURROUND, 0), 11, 12);
        var stable = SwarmTacticalHysteresisPolicy.update(pending.state(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.STANDARD, 0.5), 12, 12);
        assertEquals(SwarmAdaptiveTacticsPolicy.Pattern.STANDARD, stable.frame().pattern());
        assertNull(stable.state().pendingPattern());
        assertEquals(0.5, stable.frame().forward().x());
    }

    @Test void zeroHoldAcceptsNewPatternImmediately() {
        var first = SwarmTacticalHysteresisPolicy.update(
                SwarmTacticalHysteresisPolicy.State.empty(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.STANDARD, 1), 10, 0);
        var next = SwarmTacticalHysteresisPolicy.update(first.state(),
                frame(SwarmAdaptiveTacticsPolicy.Pattern.INTERCEPT, 0), 11, 0);
        assertTrue(next.switched());
        assertEquals(SwarmAdaptiveTacticsPolicy.Pattern.INTERCEPT, next.frame().pattern());
    }
}
