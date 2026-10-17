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
}
