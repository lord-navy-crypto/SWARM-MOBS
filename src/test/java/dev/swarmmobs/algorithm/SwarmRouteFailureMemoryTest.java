package dev.swarmmobs.algorithm;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SwarmRouteFailureMemoryTest {
    private static final long A = 40064L;
    private static final long B = 50064L;

    @Test void oneFailureDoesNotBlacklistANewlyObservedRoute() {
        var memory = new SwarmRouteFailureMemory();
        memory.reportFailure(A, 100);
        assertFalse(memory.shouldAvoid(A, 101, 2, 140));
    }

    @Test void repeatedFailuresTemporarilyAvoidTheSameCell() {
        var memory = new SwarmRouteFailureMemory();
        memory.reportFailure(A, 100);
        memory.reportFailure(A, 120);
        assertTrue(memory.shouldAvoid(A, 121, 2, 140));
        assertFalse(memory.shouldAvoid(B, 121, 2, 140));
    }

    @Test void sameTickDuplicateFailureDoesNotInflateEvidence() {
        var memory = new SwarmRouteFailureMemory();
        memory.reportFailure(A, 100);
        memory.reportFailure(A, 100);
        assertEquals(1, memory.failureCount(A, 100));
        assertFalse(memory.shouldAvoid(A, 100, 2, 140));
    }

    @Test void successfulPathEvidenceClearsTheFailure() {
        var memory = new SwarmRouteFailureMemory();
        memory.reportFailure(A, 100);
        memory.reportFailure(A, 120);
        memory.reportSuccess(A);
        assertFalse(memory.shouldAvoid(A, 121, 2, 140));
        assertEquals(0, memory.failureCount(A, 121));
    }

    @Test void failureMemoryExpiresAndRejectsTimeTravel() {
        var memory = new SwarmRouteFailureMemory();
        memory.reportFailure(A, 100);
        memory.reportFailure(A, 120);
        assertFalse(memory.shouldAvoid(A, 120 + 240, 2, 140));
        memory.reportFailure(B, 500);
        memory.reportFailure(B, 490);
        assertEquals(1, memory.failureCount(B, 500));
    }

    @Test void memoryIsBounded() {
        var memory = new SwarmRouteFailureMemory();
        for (int i = 0; i < SwarmRouteFailureMemory.MAX_ENTRIES + 100; i++) {
            memory.reportFailure(i + 1L, i + 1L);
        }
        assertTrue(memory.size(100_000L) <= SwarmRouteFailureMemory.MAX_ENTRIES);
    }
}
