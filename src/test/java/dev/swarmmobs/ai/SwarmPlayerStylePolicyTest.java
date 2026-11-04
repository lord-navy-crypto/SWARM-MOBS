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

    @Test void meleeAndExplosivePatternsUseTheirBoundedResponses() {
        var melee = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                10.0, 0.0, 0.0, 0.0, 0.0, 0.0, 10.0, 100);
        var meleeDecision = SwarmPlayerStylePolicy.choose(melee, true);
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.MELEE, meleeDecision.style());
        assertEquals(0.92, meleeDecision.formationRadiusMultiplier());
        assertEquals(0.94, meleeDecision.separationMultiplier());
        assertEquals(1.08, meleeDecision.cohesionMultiplier());

        var explosive = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                0.0, 0.0, 10.0, 0.0, 0.0, 0.0, 10.0, 100);
        var explosiveDecision = SwarmPlayerStylePolicy.choose(explosive, true);
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.EXPLOSIVE, explosiveDecision.style());
        assertEquals(1.24, explosiveDecision.formationRadiusMultiplier());
        assertEquals(1.22, explosiveDecision.separationMultiplier());
        assertEquals(0.88, explosiveDecision.cohesionMultiplier());
    }

    @Test void fireAndOtherPatternsAreExplicitlyClassified() {
        var fire = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                0.0, 0.0, 0.0, 10.0, 0.0, 0.0, 10.0, 100);
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.FIRE,
                SwarmPlayerStylePolicy.choose(fire, true).style());

        var other = new SwarmPlayerStyleRegistry.ProfileSnapshot(
                0.0, 0.0, 0.0, 0.0, 0.0, 10.0, 10.0, 100);
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.OTHER,
                SwarmPlayerStylePolicy.choose(other, true).style());
    }
}
