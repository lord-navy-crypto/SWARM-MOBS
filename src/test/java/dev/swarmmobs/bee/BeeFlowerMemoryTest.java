package dev.swarmmobs.bee;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeFlowerMemoryTest {
    @Test void sharesOnlyActuallyReportedAndRecentFlowers() {
        var m = new BeeFlowerMemory();
        assertTrue(m.suggest(11L, 101L).isEmpty());
        m.report(11L, 123L, .7, 100L);
        assertEquals(123L, m.suggest(11L, 101L).orElseThrow());
        assertTrue(m.suggest(22L, 101L).isEmpty());
        assertTrue(m.suggest(11L, 2500L).isEmpty());
    }

    @Test void prefersHighValueFreshObservationsAndHandlesInvalidFlowers() {
        var m = new BeeFlowerMemory();
        m.report(11L, 123L, .9, 1L);
        m.report(11L, 456L, .8, 1000L);
        assertEquals(456L, m.suggest(11L, 1001L).orElseThrow());
        m.forget(11L, 456L);
        assertEquals(123L, m.suggest(11L, 1001L).orElseThrow());
    }

    @Test void ephemeralCacheBoundsDoNotCapRealHiveCount() {
        var m = new BeeFlowerMemory();
        for (int hive = 0; hive < 1200; hive++) m.report(hive, hive + 8L, 1, 100);
        assertEquals(BeeFlowerMemory.MAX_CACHED_HIVES, m.rememberedHives());
        assertTrue(m.suggest(1199L, 100L).isPresent());
        for (int i = 0; i < 100; i++) m.report(1199L, 10000L + i, 1, 101L + i);
        assertEquals(BeeFlowerMemory.MAX_REMEMBERED_FLOWERS_PER_HIVE,
                16);
    }

    @Test void rejectsFictionalUtility() {
        var m = new BeeFlowerMemory();
        assertThrows(IllegalArgumentException.class,
                () -> m.report(0, 0, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> m.report(0, 0, 1.01, 0));
    }
}
