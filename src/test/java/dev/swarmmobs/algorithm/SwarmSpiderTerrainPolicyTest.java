package dev.swarmmobs.algorithm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmSpiderTerrainPolicyTest {
    @Test void spiderUsesNativeClimbForSafeVerticalWall() {
        assertTrue(SwarmSpiderTerrainPolicy.preferNativeClimb(
                true, true, true, 3, true, 0.0));
        assertTrue(SwarmSpiderTerrainPolicy.preferNativeClimb(
                true, true, true, 5, true, 4.0));
    }

    @Test void genericMobsAndNonWallClimberNavigationKeepDetours() {
        assertFalse(SwarmSpiderTerrainPolicy.preferNativeClimb(
                false, true, true, 3, true, 2.0));
        assertFalse(SwarmSpiderTerrainPolicy.preferNativeClimb(
                true, false, true, 3, true, 2.0));
    }

    @Test void unsafeOrUnsupportedWallsDoNotBypassObstacleAvoidance() {
        assertFalse(SwarmSpiderTerrainPolicy.preferNativeClimb(
                true, true, true, 1, true, 2.0));
        assertFalse(SwarmSpiderTerrainPolicy.preferNativeClimb(
                true, true, true, 6, true, 2.0));
        assertFalse(SwarmSpiderTerrainPolicy.preferNativeClimb(
                true, true, true, 3, false, 2.0));
        assertFalse(SwarmSpiderTerrainPolicy.preferNativeClimb(
                true, true, true, 3, true, -2.0));
    }

    @Test void openRoutesDoNotNeedClimbOverride() {
        assertFalse(SwarmSpiderTerrainPolicy.preferNativeClimb(
                true, true, false, 3, true, 2.0));
    }

    @Test void nonFiniteTargetHeightIsRejected() {
        assertFalse(SwarmSpiderTerrainPolicy.preferNativeClimb(
                true, true, true, 3, true, Double.NaN));
    }
}
