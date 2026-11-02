package dev.swarmmobs.bee;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeHiveColonyMemoryTest {
    @Test void reservationsCannotExceedNativeHiveCapacity() {
        var m=new BeeHiveColonyMemory(); var hive=new BlockPos(4,64,4);
        assertTrue(m.reserve(new UUID(0,1),hive,0,100));
        assertTrue(m.reserve(new UUID(0,2),hive,0,100));
        assertTrue(m.reserve(new UUID(0,3),hive,0,100));
        assertFalse(m.reserve(new UUID(0,4),hive,0,100));
        assertEquals(3,m.reservedSlots(hive.asLong(),100));
    }
    @Test void reservationsAreIdempotentAndFollowBeeHome() {
        var m=new BeeHiveColonyMemory(); UUID bee=new UUID(0,9);
        var first=new BlockPos(1,64,1); var second=new BlockPos(2,64,1);
        assertTrue(m.reserve(bee,first,0,100));
        assertTrue(m.reserve(bee,first,0,101));
        assertEquals(1,m.reservedSlots(first.asLong(),101));
        m.reconcile(bee,second,102);
        assertEquals(0,m.reservedSlots(first.asLong(),102));
    }
    @Test void reservationsExpire() {
        var m=new BeeHiveColonyMemory(); var hive=new BlockPos(0,64,0);
        assertTrue(m.reserve(new UUID(0,1),hive,0,100));
        assertEquals(1,m.reservationCount(100));
        assertEquals(0,m.reservationCount(100+BeeHiveColonyMemory.RESERVATION_TTL_TICKS));
    }
}
