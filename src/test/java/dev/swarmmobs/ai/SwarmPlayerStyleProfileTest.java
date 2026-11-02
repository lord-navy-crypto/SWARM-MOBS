package dev.swarmmobs.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SwarmPlayerStyleProfileTest {
    private static final double EPSILON = 1.0e-9;

    @Test
    void categoryCountsAndEffectiveSampleMassDecayByTheConfiguredHalfLife() {
        var profile = new SwarmPlayerStyleProfile(100);
        profile.observe(SwarmPlayerStyleRegistry.AttackStyle.MELEE, 100);
        profile.observe(SwarmPlayerStyleRegistry.AttackStyle.PROJECTILE, 100);

        var snapshot = profile.snapshot(2500);

        assertEquals(0.5, snapshot.melee(), EPSILON);
        assertEquals(0.5, snapshot.projectile(), EPSILON);
        assertEquals(1.0, snapshot.sampleMass(), EPSILON);
        assertEquals(100, snapshot.lastObservedTick());
    }

    @Test
    void uniformDecayPreservesTheObservedCategoryShares() {
        var profile = new SwarmPlayerStyleProfile(0);
        for (int i = 0; i < 6; i++) {
            profile.observe(SwarmPlayerStyleRegistry.AttackStyle.MELEE, 0);
        }
        for (int i = 0; i < 4; i++) {
            profile.observe(SwarmPlayerStyleRegistry.AttackStyle.PROJECTILE, 0);
        }

        var snapshot = profile.snapshot(SwarmPlayerStyleProfile.HALF_LIFE_TICKS);

        assertEquals(3.0, snapshot.melee(), EPSILON);
        assertEquals(2.0, snapshot.projectile(), EPSILON);
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.MELEE, snapshot.dominantStyle());
    }

    @Test
    void decayedEvidenceCanFallBelowTheMinimumConfidenceThreshold() {
        var profile = new SwarmPlayerStyleProfile(0);
        for (int i = 0; i < 5; i++) {
            profile.observe(SwarmPlayerStyleRegistry.AttackStyle.MELEE, 0);
        }

        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.MELEE,
                profile.snapshot(0).dominantStyle());
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.UNKNOWN,
                profile.snapshot(SwarmPlayerStyleProfile.HALF_LIFE_TICKS).dominantStyle());
    }

    @Test
    void aHighShareStillNeedsEnoughMarginOverTheSecondCategory() {
        var narrowLead = new SwarmPlayerStyleProfile(0);
        for (int i = 0; i < 11; i++) {
            narrowLead.observe(SwarmPlayerStyleRegistry.AttackStyle.PROJECTILE, 0);
        }
        for (int i = 0; i < 9; i++) {
            narrowLead.observe(SwarmPlayerStyleRegistry.AttackStyle.MELEE, 0);
        }
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.MIXED,
                narrowLead.snapshot(0).dominantStyle());

        var clearLead = new SwarmPlayerStyleProfile(0);
        for (int i = 0; i < 12; i++) {
            clearLead.observe(SwarmPlayerStyleRegistry.AttackStyle.PROJECTILE, 0);
        }
        for (int i = 0; i < 8; i++) {
            clearLead.observe(SwarmPlayerStyleRegistry.AttackStyle.MELEE, 0);
        }
        assertEquals(SwarmPlayerStyleRegistry.AttackStyle.PROJECTILE,
                clearLead.snapshot(0).dominantStyle());
    }

    @Test
    void profileExpiresOnlyAfterTheFullInactivityWindow() {
        var profile = new SwarmPlayerStyleProfile(200);
        profile.observe(SwarmPlayerStyleRegistry.AttackStyle.MAGIC, 200);

        assertFalse(profile.isExpired(200 + SwarmPlayerStyleProfile.PROFILE_TTL_TICKS));
        assertTrue(profile.isExpired(201 + SwarmPlayerStyleProfile.PROFILE_TTL_TICKS));
    }

    @Test
    void aBackwardsTickDoesNotUndoPreviouslyAppliedDecay() {
        var profile = new SwarmPlayerStyleProfile(100);
        profile.observe(SwarmPlayerStyleRegistry.AttackStyle.FIRE, 100);

        assertEquals(0.5, profile.snapshot(2500).fire(), EPSILON);
        assertEquals(0.5, profile.snapshot(2400).fire(), EPSILON);
    }

    @Test
    void nonObservationStylesDoNotAddEvidence() {
        var profile = new SwarmPlayerStyleProfile(0);
        profile.observe(SwarmPlayerStyleRegistry.AttackStyle.MIXED, 0);
        profile.observe(SwarmPlayerStyleRegistry.AttackStyle.UNKNOWN, 0);

        assertEquals(0.0, profile.snapshot(0).sampleMass(), EPSILON);
    }
}
