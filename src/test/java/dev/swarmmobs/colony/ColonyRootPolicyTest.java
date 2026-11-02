package dev.swarmmobs.colony;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColonyRootPolicyTest {
    private static ColonyRootPolicy.Core core(int x, boolean active, long resources) {
        return new ColonyRootPolicy.Core(new ConnectedColonyLayout.Cell(x, 64, 0),
                active, resources);
    }

    @Test void oneActiveCoreOwnsPassiveExtensions() {
        var election = ColonyRootPolicy.elect(List.of(
                core(0, true, 72), core(1, false, 0), core(2, false, 0)));
        assertFalse(election.conflict());
        assertEquals(new ConnectedColonyLayout.Cell(0, 64, 0), election.root());
        assertEquals(2, election.extensions());
    }

    @Test void twoOriginalCoresDoNotMergeInventories() {
        assertTrue(ColonyRootPolicy.elect(List.of(
                core(0, true, 88), core(1, true, 110))).conflict());
    }

    @Test void orphanedExtensionsNeverBecomeFreeSpawners() {
        assertTrue(ColonyRootPolicy.elect(List.of(
                core(1, false, 0), core(2, false, 0))).conflict());
    }

    @Test void largeColonyCapacityHasNoArbitraryExtensionCeiling() {
        assertEquals(4_000_004L, ColonyRootPolicy.totalCapacity(4, 1_000_000L, 4));
    }

    @Test void capacityArithmeticRejectsOverflow() {
        assertThrows(ArithmeticException.class,
                () -> ColonyRootPolicy.totalCapacity(4, Long.MAX_VALUE, 4));
    }
}
