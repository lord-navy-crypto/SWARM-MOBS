package dev.swarmmobs.bee;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeHiveColonyMemoryTest {
    @Test void reservationsCannotExceedNativeHiveCapacity() {
        var m = new BeeHiveSlotReservations(); long hive = 1234L;
        assertTrue(m.reserve(new UUID(0,1), hive, 0, 100, 3));
        assertTrue(m.reserve(new UUID(0,2), hive, 0, 100, 3));
        assertTrue(m.reserve(new UUID(0,3), hive, 0, 100, 3));
        assertFalse(m.reserve(new UUID(0,4), hive, 0, 100, 3));
        assertEquals(3, m.reservedSlots(hive, 100));
    }
    @Test void reservationsAreIdempotentAndFollowBeeHome() {
        var m = new BeeHiveSlotReservations(); UUID bee = new UUID(0,9);
        long first = 11L, second = 12L;
        assertTrue(m.reserve(bee, first, 0, 100, 3));
        assertTrue(m.reserve(bee, first, 0, 101, 3));
        assertEquals(1, m.reservedSlots(first, 101));
        m.reconcile(bee, second, 102);
        assertEquals(0, m.reservedSlots(first, 102));
    }
    @Test void reservationsExpireAndRemainBounded() {
        var m = new BeeHiveSlotReservations(); long hive = 0L;
        assertTrue(m.reserve(new UUID(0,1), hive, 0, 100, 3));
        assertEquals(1, m.reservationCount(100));
        assertEquals(0, m.reservationCount(100 + BeeHiveSlotReservations.RESERVATION_TTL_TICKS));
        for (int i=0; i<BeeHiveSlotReservations.MAX_RESERVATIONS+10; i++)
            m.reserve(new UUID(1,i), hive, 0, 20000L+i, 3);
        assertTrue(m.reservationCount(30000) <= BeeHiveSlotReservations.MAX_RESERVATIONS);
    }
    @Test void invalidReservationsAreRejected() {
        var m = new BeeHiveSlotReservations();
        assertThrows(IllegalArgumentException.class, () -> m.reserve(null, 0L, 0, 1, 3));
        assertThrows(IllegalArgumentException.class, () -> m.reserve(new UUID(0,1), 0L, 4, 1, 3));
    }
}
