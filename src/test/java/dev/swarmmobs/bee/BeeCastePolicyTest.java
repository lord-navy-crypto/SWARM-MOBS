package dev.swarmmobs.bee;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeCastePolicyTest {
    @Test void casteAssignmentIsStableAndProfilesRemainBounded() {
        UUID bee = new UUID(0x1234L, 0x5678L);
        assertEquals(BeeCastePolicy.caste(bee), BeeCastePolicy.caste(bee));
        assertNotNull(BeeCastePolicy.workerProfile(bee));
        for (int i = 0; i < 100; i++) {
            var profile = BeeCastePolicy.workerProfile(new UUID(0, i));
            assertTrue(profile.foragingBias() >= 0 && profile.foragingBias() <= 1);
            assertTrue(profile.nursingBias() >= 0 && profile.nursingBias() <= 1);
            assertTrue(profile.guardingBias() >= 0 && profile.guardingBias() <= 1);
            assertTrue(profile.buildingBias() >= 0 && profile.buildingBias() <= 1);
        }
    }
}
