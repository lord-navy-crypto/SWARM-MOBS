package dev.swarmmobs.algorithm;

import dev.swarmmobs.agent.SwarmAgentArchetype;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmSpeciesNavigationPolicyTest {
    @Test void eachSpeciesHasDistinctLocomotionTuning() {
        var zombie = SwarmSpeciesNavigationPolicy.profile(SwarmAgentArchetype.ASSAULT);
        var skeleton = SwarmSpeciesNavigationPolicy.profile(SwarmAgentArchetype.RANGED_SUPPORT);
        var spider = SwarmSpeciesNavigationPolicy.profile(SwarmAgentArchetype.FLANKER);
        var creeper = SwarmSpeciesNavigationPolicy.profile(SwarmAgentArchetype.BREACHER);

        assertEquals(1.0, zombie.obstacleLateralScale(), 1.0e-9);
        assertTrue(skeleton.congestionPenaltyScale() > zombie.congestionPenaltyScale());
        assertTrue(spider.recoveryLateralScale() > zombie.recoveryLateralScale());
        assertTrue(creeper.failureMemoryTicks() > spider.failureMemoryTicks());
    }

    @Test void allProfilesRemainWithinPositiveBoundedValues() {
        for (var archetype : SwarmAgentArchetype.values()) {
            var profile = SwarmSpeciesNavigationPolicy.profile(archetype);
            assertTrue(profile.obstacleLateralScale() > 0);
            assertTrue(profile.recoveryLateralScale() > 0);
            assertTrue(profile.congestionPenaltyScale() >= 0);
            assertTrue(profile.repeatedFailureThreshold() >= 1);
            assertTrue(profile.failureMemoryTicks() > 0);
        }
    }

    @Test void invalidProfilesAreRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                new SwarmSpeciesNavigationPolicy.Profile(0, 1, 1, 1, 10));
        assertThrows(IllegalArgumentException.class, () ->
                new SwarmSpeciesNavigationPolicy.Profile(1, 1, -1, 1, 10));
    }
}
