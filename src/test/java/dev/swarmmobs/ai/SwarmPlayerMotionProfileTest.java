package dev.swarmmobs.ai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmPlayerMotionProfileTest {
    @Test void repeatedDirectionChangesBecomeErraticEvidence() {
        var profile = new SwarmPlayerMotionProfile(0);
        for (int tick = 1; tick <= 10; tick++) {
            double vx = tick % 2 == 0 ? -0.30 : 0.30;
            profile.observe(vx, 0.0, tick, tick);
        }
        var snapshot = profile.snapshot(10);
        assertEquals(SwarmPlayerMotionRegistry.MotionStyle.ERRATIC, snapshot.dominantStyle());
        assertTrue(snapshot.erratic() >= 8.0);
    }

    @Test void stationaryAndWalkingMotionAreDistinguished() {
        var still = new SwarmPlayerMotionProfile(0);
        for (int tick = 1; tick <= 6; tick++) still.observe(0.0, 0.0, tick, tick);
        assertEquals(SwarmPlayerMotionRegistry.MotionStyle.STATIONARY,
                still.snapshot(6).dominantStyle());

        var walking = new SwarmPlayerMotionProfile(0);
        for (int tick = 1; tick <= 6; tick++) walking.observe(0.10, 0.0, tick, tick);
        assertEquals(SwarmPlayerMotionRegistry.MotionStyle.WALKING,
                walking.snapshot(6).dominantStyle());
    }

    @Test void duplicateOrOlderObservationsDoNotInflateEvidence() {
        var profile = new SwarmPlayerMotionProfile(0);
        profile.observe(0.25, 0.0, 10, 10);
        profile.observe(-0.25, 0.0, 10, 11);
        profile.observe(-0.25, 0.0, 9, 12);
        assertEquals(1.0, profile.snapshot(10).sampleMass(), 1e-9);
    }

    @Test void evidenceDecaysAndExpiresAfterInactivity() {
        var profile = new SwarmPlayerMotionProfile(0);
        for (int tick = 1; tick <= 6; tick++) profile.observe(0.25, 0.0, tick, 6);
        assertEquals(6.0, profile.snapshot(0).sampleMass(), 1e-9);
        assertEquals(3.0, profile.snapshot(2406).sampleMass(), 1e-6);
        assertTrue(profile.isExpired(12007));
    }
}
