package dev.swarmmobs.bee;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeHiveAlarmMemoryTest {
    private static final UUID THREAT = new UUID(0, 1);
    private static final UUID OTHER = new UUID(0, 2);

    @Test void scanClaimsAreThrottledPerHive() {
        var memory = new BeeHiveAlarmMemory();
        assertTrue(memory.claimScan(10, 100, 20));
        assertFalse(memory.claimScan(10, 110, 20));
        assertTrue(memory.claimScan(10, 120, 20));
        assertTrue(memory.claimScan(11, 120, 20));
    }

    @Test void repeatedLocalObservationsIncreaseAlarmSeverity() {
        var memory = new BeeHiveAlarmMemory();
        memory.report(10, THREAT, 101, 100);
        assertEquals(1, memory.active(10, 100, 120).orElseThrow().severity());
        memory.report(10, THREAT, 102, 120);
        assertEquals(2, memory.active(10, 120, 120).orElseThrow().severity());
        memory.report(10, THREAT, 103, 140);
        assertEquals(3, memory.active(10, 140, 120).orElseThrow().severity());
    }

    @Test void differentThreatStartsANewIncident() {
        var memory = new BeeHiveAlarmMemory();
        memory.report(10, THREAT, 101, 100);
        memory.report(10, OTHER, 102, 120);
        var alert = memory.active(10, 120, 120).orElseThrow();
        assertEquals(OTHER, alert.observedThreat());
        assertEquals(1, alert.severity());
    }

    @Test void alarmExpiresAndIsHiveLocal() {
        var memory = new BeeHiveAlarmMemory();
        memory.report(10, THREAT, 101, 100);
        assertTrue(memory.active(10, 219, 120).isPresent());
        assertTrue(memory.active(10, 220, 120).isEmpty());
        assertTrue(memory.active(11, 120, 120).isEmpty());
    }

    @Test void alarmAndScanMemoryRemainBounded() {
        var memory = new BeeHiveAlarmMemory();
        for (int i = 0; i < BeeHiveAlarmMemory.MAX_HIVES + 50; i++) {
            memory.report(i, new UUID(0, i + 1L), i, 100);
            memory.claimScan(i, 100, 20);
        }
        assertTrue(memory.trackedHives() <= BeeHiveAlarmMemory.MAX_HIVES);
    }

    @Test void invalidReportsAndClockIntervalsAreRejected() {
        var memory = new BeeHiveAlarmMemory();
        assertThrows(IllegalArgumentException.class, () -> memory.report(1, null, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> memory.report(1, THREAT, 1, -1));
        assertThrows(IllegalArgumentException.class, () -> memory.claimScan(1, 0, 0));
    }
}
