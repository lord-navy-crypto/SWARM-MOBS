package dev.swarmmobs.colony;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColonyRallyPolicyTest {
    private final UUID volunteer = new UUID(0L, 1L);

    @Test void onlyReasonablyNearbyIdleMembersCanMuster() {
        assertTrue(ColonyRallyPolicy.mayRally(volunteer, true, true, true, 20 * 20));
        assertFalse(ColonyRallyPolicy.mayRally(volunteer, false, true, true, 400));
        assertFalse(ColonyRallyPolicy.mayRally(volunteer, true, false, true, 400));
        assertFalse(ColonyRallyPolicy.mayRally(volunteer, true, true, false, 400));
        assertFalse(ColonyRallyPolicy.mayRally(volunteer, true, true, true, 50 * 50));
        assertFalse(ColonyRallyPolicy.mayRally(volunteer, true, true, true, 4));
    }

    @Test void limitedRecruitmentIsStableForMemberIdentity() {
        assertFalse(ColonyRallyPolicy.mayRally(new UUID(0L, 0L), true, true, true, 400));
        assertTrue(ColonyRallyPolicy.mayRally(volunteer, true, true, true, 400));
        assertEquals(ColonyRallyPolicy.mayRally(volunteer, true, true, true, 400),
                ColonyRallyPolicy.mayRally(volunteer, true, true, true, 400));
    }
}
