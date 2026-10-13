package dev.swarmmobs.colony;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IncrementalColonyIndexTest {
    @Test void growsPastOldComponentLimitAcrossBoundedSteps() {
        var start = new ConnectedColonyLayout.Cell(0, 64, 0);
        var index = new IncrementalColonyIndex(start);
        for (int i = 0; i < 1000 && !index.completeForLoadedRegion(); i++) {
            assertTrue(index.step(4, c -> true,
                    c -> c.y() == 64 && c.z() == 0 && c.x() >= 0 && c.x() < 300) <= 4);
        }
        assertEquals(300, index.knownBlocks());
        assertEquals(900L, index.capacity(3));
    }

    @Test void defersUnloadedCellsWithoutForcingReads() {
        var start = new ConnectedColonyLayout.Cell(0, 64, 0);
        var other = new ConnectedColonyLayout.Cell(1, 64, 0);
        var index = new IncrementalColonyIndex(start);
        index.step(10, c -> !c.equals(other),
                c -> { if (c.equals(other)) fail("read unloaded cell"); return c.equals(start); });
        assertEquals(1, index.knownBlocks());
        assertTrue(index.hasDeferredFrontier());
        index.retryDeferred();
        for (int i = 0; i < 5; i++) {
            index.step(8, c -> true, c -> c.equals(start) || c.equals(other));
        }
        assertEquals(2, index.knownBlocks());
    }

    @Test void resetCorrectlyDropsOldMembership() {
        var start = new ConnectedColonyLayout.Cell(0, 64, 0);
        var next = new ConnectedColonyLayout.Cell(1, 64, 0);
        var index = new IncrementalColonyIndex(start);
        index.step(20, c -> true, c -> c.equals(start) || c.equals(next));
        assertEquals(2, index.knownBlocks());
        index.reset(start);
        index.step(20, c -> true, start::equals);
        assertEquals(1, index.knownBlocks());
    }

    @Test void diagonalIsNotConnected() {
        var start = new ConnectedColonyLayout.Cell(0, 64, 0);
        var index = new IncrementalColonyIndex(start);
        var blocks = Set.of(start, new ConnectedColonyLayout.Cell(1, 65, 0));
        index.step(20, c -> true, blocks::contains);
        assertEquals(1, index.knownBlocks());
    }
}
