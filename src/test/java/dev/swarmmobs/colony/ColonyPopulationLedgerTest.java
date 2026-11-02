package dev.swarmmobs.colony;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColonyPopulationLedgerTest {
    @Test void oneMobReportedManyTimesIsOneMember() {
        var registry = new ColonyPopulationLedger();
        UUID id = UUID.randomUUID();
        for (int tick = 10; tick < 200; tick++)
            registry.observe(101L, id, tick);
        assertEquals(1, registry.count(101L, 201L, 400L));
    }

    @Test void distantMembersShareOnlyTheirAssignedRoot() {
        var registry = new ColonyPopulationLedger();
        registry.observe(101L, UUID.randomUUID(), 500L);
        registry.observe(101L, UUID.randomUUID(), 500L);
        registry.observe(202L, UUID.randomUUID(), 500L);
        assertEquals(2, registry.count(101L, 500L, 400L));
        assertEquals(1, registry.count(202L, 500L, 400L));
    }

    @Test void staleOrUnloadedMembersExpireAutomatically() {
        var registry = new ColonyPopulationLedger();
        UUID id = UUID.randomUUID();
        registry.observe(101L, id, 100L);
        assertEquals(1, registry.count(101L, 499L, 400L));
        assertEquals(0, registry.count(101L, 500L, 400L));
        assertEquals(0, registry.trackedRoots());
    }

    @Test void observationsNeverMoveBackwardsInTime() {
        var registry = new ColonyPopulationLedger();
        UUID id = UUID.randomUUID();
        registry.observe(101L, id, 101L);
        registry.observe(101L, id, 100L);
        assertEquals(1, registry.count(101L, 101L, 2L));
    }

    @Test void supportsLargeGroupsWithoutArtificialMemberCap() {
        var registry = new ColonyPopulationLedger();
        for (int i = 0; i < 10000; i++)
            registry.observe(101L, new UUID(0L, i), 1000L);
        assertEquals(10000, registry.count(101L, 1001L, 400L));
    }
}
