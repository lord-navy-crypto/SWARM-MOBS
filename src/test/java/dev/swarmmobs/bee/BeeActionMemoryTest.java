package dev.swarmmobs.bee;

import dev.swarmmobs.visual.SwarmAnimationCue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BeeActionMemoryTest {
    private static final UUID BEE = new UUID(0, 77);

    @Test
    void identicalCueIsRateLimitedPerBee() {
        var memory = new BeeActionMemory();
        assertTrue(memory.claim(BEE, SwarmAnimationCue.BEE_GUARD_ALERT, 100, 40));
        assertFalse(memory.claim(BEE, SwarmAnimationCue.BEE_GUARD_ALERT, 120, 40));
        assertTrue(memory.claim(BEE, SwarmAnimationCue.BEE_GUARD_ALERT, 140, 40));
    }

    @Test
    void differentBeesAndDifferentCuesHaveIndependentCooldowns() {
        var memory = new BeeActionMemory();
        UUID other = new UUID(0, 78);
        assertTrue(memory.claim(BEE, SwarmAnimationCue.BEE_GUARD_ALERT, 100, 80));
        assertTrue(memory.claim(other, SwarmAnimationCue.BEE_GUARD_ALERT, 100, 80));
        assertTrue(memory.claim(BEE, SwarmAnimationCue.BEE_POLLINATE, 100, 30));
    }

    @Test
    void expiredEntriesArePrunedAndMemoryIsBounded() {
        var memory = new BeeActionMemory();
        assertTrue(memory.claim(BEE, SwarmAnimationCue.BEE_GUARD_ALERT, 100, 10));
        assertEquals(0, memory.size(100 + BeeActionMemory.MEMORY_TICKS));
        for (int i = 0; i < BeeActionMemory.MAX_CUES + 10; i++) {
            memory.claim(new UUID(1, i), SwarmAnimationCue.BEE_REST, 20_000L + i, 0);
        }
        assertTrue(memory.size(40_000L) <= BeeActionMemory.MAX_CUES);
    }

    @Test
    void invalidCueClaimsAreRejected() {
        var memory = new BeeActionMemory();
        assertThrows(IllegalArgumentException.class,
                () -> memory.claim(null, SwarmAnimationCue.BEE_REST, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> memory.claim(BEE, null, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> memory.claim(BEE, SwarmAnimationCue.BEE_REST, -1, 1));
    }
}
