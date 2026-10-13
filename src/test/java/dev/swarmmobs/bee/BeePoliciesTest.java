package dev.swarmmobs.bee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeePoliciesTest {
    @Test
    void forageScoringPrefersSaferCloserPatch() {
        var near = new BeeForagePolicy.Patch(0.9, 0.8, 12, 0.1, 0, 1);
        var far = new BeeForagePolicy.Patch(0.9, 0.8, 100, 0.7, 0.5, 1);
        assertTrue(BeeForagePolicy.score(near) > BeeForagePolicy.score(far));
    }

    @Test
    void rememberedInformationDecaysWithoutRemoteUpdates() {
        assertEquals(0.5, BeeForagePolicy.confidenceAfter(200, 1.0, 200), 1e-12);
        assertEquals(0.25, BeeForagePolicy.confidenceAfter(400, 1.0, 200), 1e-12);
    }

    @Test
    void taskAllocationRespondsToThreatAndUsesSwitchHysteresis() {
        var worker = new BeeTaskPolicy.Worker(1, 1, 1, 1);
        var needs = new BeeTaskPolicy.Needs(0.4, 0.2, 0.95, 0.1, 0);
        assertEquals(BeeTaskPolicy.Task.GUARD,
                BeeTaskPolicy.select(needs, worker, BeeTaskPolicy.Task.FORAGE, 0.1));
        assertEquals(BeeTaskPolicy.Task.FORAGE,
                BeeTaskPolicy.select(needs, worker, BeeTaskPolicy.Task.FORAGE, 0.6));
    }

    @Test
    void invalidSensorInputsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new BeeForagePolicy.Patch(2, 0, 1, 0, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new BeeTaskPolicy.Needs(Double.NaN, 0, 0, 0, 0));
    }
}
