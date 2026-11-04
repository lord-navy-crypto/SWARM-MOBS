package dev.swarmmobs.ai;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmPlayerMotionPolicyTest {
    @Test void lowEvidenceDoesNotChangeBaseline() {
        var profile = new SwarmPlayerMotionRegistry.ProfileSnapshot(0, 0, 4, 0, 4, 10);
        var decision = SwarmPlayerMotionPolicy.choose(profile, true);
        assertEquals(SwarmPlayerMotionRegistry.MotionStyle.UNKNOWN, decision.style());
        assertEquals(1.0, decision.formationRadiusMultiplier());
    }

    @Test void erraticMovementWidensSpacingAndSearchCoverage() {
        var profile = new SwarmPlayerMotionRegistry.ProfileSnapshot(0, 0, 1, 12, 13, 10);
        var decision = SwarmPlayerMotionPolicy.choose(profile, true);
        assertEquals(SwarmPlayerMotionRegistry.MotionStyle.ERRATIC, decision.style());
        assertEquals(1.18, decision.formationRadiusMultiplier());
        assertEquals(1.15, decision.separationMultiplier());
        assertEquals(0.88, decision.cohesionMultiplier());
        assertEquals(1.18, decision.searchRadiusMultiplier());
    }

    @Test void stationaryMovementKeepsTheGroupCompact() {
        var profile = new SwarmPlayerMotionRegistry.ProfileSnapshot(10, 0, 0, 0, 10, 10);
        var decision = SwarmPlayerMotionPolicy.choose(profile, true);
        assertEquals(SwarmPlayerMotionRegistry.MotionStyle.STATIONARY, decision.style());
        assertEquals(0.94, decision.formationRadiusMultiplier());
        assertEquals(1.06, decision.cohesionMultiplier());
    }

    @Test void disabledLearningReturnsBaseline() {
        var profile = new SwarmPlayerMotionRegistry.ProfileSnapshot(0, 0, 10, 0, 10, 10);
        assertEquals(1.0, SwarmPlayerMotionPolicy.choose(profile, false)
                .searchRadiusMultiplier());
    }
}
