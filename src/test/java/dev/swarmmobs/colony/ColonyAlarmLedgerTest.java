package dev.swarmmobs.colony;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColonyAlarmLedgerTest {
    @Test void activeAlarmPreemptsCivilianWindow() {
        var alarms = new ColonyAlarmLedger();
        alarms.report(12L, 100L);
        assertTrue(alarms.active(12L, 120L, 200L));
        assertFalse(alarms.active(44L, 120L, 200L));
        assertTrue(alarms.active(12L, 299L, 200L));
        assertFalse(alarms.active(12L, 300L, 200L));
    }

    @Test void repeatedSignalsRefreshRatherThanShortenAlarm() {
        var alarms = new ColonyAlarmLedger();
        alarms.report(3L, 100L);
        alarms.report(3L, 90L);
        alarms.report(3L, 180L);
        assertTrue(alarms.active(3L, 300L, 200L));
        assertFalse(alarms.active(3L, 380L, 200L));
        assertEquals(0, alarms.trackedRoots());
    }

    @Test void expiredOrRestartedClockCannotKeepAnAlarmForever() {
        var alarms = new ColonyAlarmLedger();
        alarms.report(3L, 200L);
        assertFalse(alarms.active(3L, 199L, 200L));
        assertEquals(0, alarms.trackedRoots());
    }
    @Test void rallyUsesActualWitnessNotEnemyCoordinates() {
        var alarms = new ColonyAlarmLedger();
        alarms.report(9L, 100L, 12345678L);
        assertEquals(12345678L,
                alarms.witnessPosition(9L, 102L, 200L).orElseThrow());
        alarms.report(9L, 101L);
        assertTrue(alarms.active(9L, 101L, 200L));
        assertTrue(alarms.witnessPosition(9L, 101L, 200L).isEmpty());
        alarms.report(9L, 120L, 9988L);
        assertEquals(9988L,
                alarms.witnessPosition(9L, 121L, 200L).orElseThrow());
        assertTrue(alarms.witnessPosition(9L, 400L, 200L).isEmpty());
    }
}
