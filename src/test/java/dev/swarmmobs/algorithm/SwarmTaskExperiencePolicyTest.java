package dev.swarmmobs.algorithm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SwarmTaskExperiencePolicyTest {

    @Test
    void oneReferenceIntervalPreservesOriginalGainAndRetention() {
        assertEquals(0.224, SwarmTaskExperiencePolicy.advance(
                0.20, 0.025, 0.995, 6), 1.0e-12);
    }

    @Test
    void elapsedTimeProducesSameResultAcrossDifferentPlannerCadences() {
        double oneLongUpdate = SwarmTaskExperiencePolicy.advance(
                0.20, 0.025, 0.995, 12);

        double twoUpdates = SwarmTaskExperiencePolicy.advance(
                SwarmTaskExperiencePolicy.advance(0.20, 0.025, 0.995, 6),
                0.025, 0.995, 6);

        double sixUpdates = 0.20;
        for (int i = 0; i < 6; i++) {
            sixUpdates = SwarmTaskExperiencePolicy.advance(
                    sixUpdates, 0.025, 0.995, 2);
        }

        assertEquals(oneLongUpdate, twoUpdates, 1.0e-12);
        assertEquals(oneLongUpdate, sixUpdates, 1.0e-12);
    }

    @Test
    void inactiveTaskDecaysByElapsedTimeRatherThanUpdateCount() {
        double afterTwelveTicks = SwarmTaskExperiencePolicy.advance(
                0.80, 0.0, 0.90, 12);
        assertEquals(0.80 * Math.pow(0.90, 2.0), afterTwelveTicks, 1.0e-12);
    }

    @Test
    void duplicateAndBackwardTicksDoNotChangeExperience() {
        assertEquals(0.42, SwarmTaskExperiencePolicy.advance(
                0.42, 0.20, 0.90, 0), 1.0e-12);
        assertEquals(0.42, SwarmTaskExperiencePolicy.advance(
                0.42, 0.20, 0.90, -10), 1.0e-12);
    }

    @Test
    void catchUpIsBoundedAndValuesRemainFinite() {
        double bounded = SwarmTaskExperiencePolicy.advance(
                0.10, 0.025, 0.995, 10_000);
        double maximumCatchUp = SwarmTaskExperiencePolicy.advance(
                0.10, 0.025, 0.995, 40);
        assertEquals(maximumCatchUp, bounded, 1.0e-12);

        assertEquals(0.0, SwarmTaskExperiencePolicy.advance(
                Double.NaN, Double.NaN, Double.NaN, 6), 1.0e-12);
        assertEquals(1.0, SwarmTaskExperiencePolicy.advance(
                1.0, 0.25, 0.90, 40), 1.0e-12);
    }
}
