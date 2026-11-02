package dev.swarmmobs.ai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmPlayerStylePolicyTest {
    @Test void insufficientSamplesDoNotChangeTheBaseline() {
        var profile = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                4.0, 0.0, 0.0, 0.0, 0.0, 0.0, 4.0, 100);
        var decision = SwarmPlayerStylePolicy.choose(profile, true);
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.UNKNOWN, decision.style());
        assertEquals(1.0, decision.formationRadiusMultiplier());
    }

    @Test void aClearProjectilePatternWidensTheFormationWithinBounds() {
        var profile = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                1.0, 12.0, 0.0, 0.0, 0.0, 0.0, 13.0, 100);
        var decision = SwarmPlayerStylePolicy.choose(profile, true);
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.PROJECTILE, decision.style());
        assertEquals(1.18, decision.formationRadiusMultiplier());
        assertEquals(1.16, decision.separationMultiplier());
        assertEquals(0.92, decision.cohesionMultiplier());
    }

    @Test void magicIsASeparateLearnedCategory() {
        var profile = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                0.0, 0.0, 0.0, 0.0, 9.0, 0.0, 9.0, 100);
        var decision = SwarmPlayerStylePolicy.choose(profile, true);
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.MAGIC, decision.style());
    }

    @Test void aSmallLeadOverTheSecondPatternIsNotTreatedAsDominant() {
        var profile = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                10.0, 8.0, 0.0, 0.0, 0.0, 0.0, 18.0, 100);
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.MIXED, profile.dominantStyle());
    }

    @Test void mixedPatternsAndDisabledLearningRemainBaseline() {
        var mixed = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                6.0, 5.0, 0.0, 0.0, 0.0, 0.0, 11.0, 100);
        assertEquals(1.0, SwarmPlayerStylePolicy.choose(mixed, true)
                .formationRadiusMultiplier());
        var projectile = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                0.0, 10.0, 0.0, 0.0, 0.0, 0.0, 10.0, 100);
        assertEquals(1.0, SwarmPlayerStylePolicy.choose(projectile, false)
                .separationMultiplier());
    }
}
