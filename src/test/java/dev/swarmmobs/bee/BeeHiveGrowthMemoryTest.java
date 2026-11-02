package dev.swarmmobs.bee;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BeeHiveGrowthMemoryTest {
    private static final List<Long> TWO=List.of(0L, 1L);
    @Test void connectedHivesShareSameTickClaim() {
        var m=new BeeHiveGrowthMemory();
        assertTrue(m.claimColony(TWO,100,2400));
        assertFalse(m.claimColony(TWO,100,2400));
        assertTrue(m.claimColony(TWO,101,2400));
    }
    @Test void successfulGrowthCooldownAppliesToNewExtensions() {
        var m=new BeeHiveGrowthMemory();
        assertTrue(m.claimColony(TWO,100,2400));
        m.recordExpansion(TWO,100);
        var three=List.of(TWO.get(0), TWO.get(1), 2L);
        assertFalse(m.claimColony(three,2499,2400));
        assertTrue(m.claimColony(three,2500,2400));
    }
    @Test void entriesExpireAndMemoryStaysBounded() {
        var m=new BeeHiveGrowthMemory();
        assertTrue(m.claimColony(TWO,100,2400));
        assertEquals(2,m.size(100));
        assertEquals(0,m.size(100+BeeHiveGrowthMemory.MEMORY_TICKS));
        for(int i=0;i<BeeHiveGrowthMemory.MAX_TRACKED_HIVES+10;i++)
            m.claim((long) i, 20000L+i);
        assertTrue(m.size(30000)<=BeeHiveGrowthMemory.MAX_TRACKED_HIVES);
    }
}
