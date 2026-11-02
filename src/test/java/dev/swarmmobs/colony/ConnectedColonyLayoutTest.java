package dev.swarmmobs.colony;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConnectedColonyLayoutTest {
    @Test void faceConnectedBlocksBecomeOneNest() {
        var blocks = Set.of(new ConnectedColonyLayout.Cell(0, 64, 0),
                new ConnectedColonyLayout.Cell(1, 64, 0),
                new ConnectedColonyLayout.Cell(1, 65, 0));
        var scanned = ConnectedColonyLayout.scan(new ConnectedColonyLayout.Cell(0, 64, 0),
                blocks::contains, 8, 3);
        assertEquals(3, scanned.cells().size());
        assertEquals(9, scanned.capacity());
    }

    @Test void diagonalAndSeparatedBlocksStaySeparate() {
        var origin = new ConnectedColonyLayout.Cell(0, 64, 0);
        var blocks = Set.of(origin, new ConnectedColonyLayout.Cell(1, 65, 0),
                new ConnectedColonyLayout.Cell(4, 64, 0));
        assertEquals(1, ConnectedColonyLayout.scan(origin, blocks::contains, 8, 3).cells().size());
    }

    @Test void growthSelectsAirBesideTheExistingStructure() {
        var origin = new ConnectedColonyLayout.Cell(0, 64, 0);
        var colony = ConnectedColonyLayout.scan(origin, origin::equals, 8, 3);
        assertEquals(new ConnectedColonyLayout.Cell(-1, 64, 0),
                ConnectedColonyLayout.nextExtension(colony,
                    cell -> cell.y() == 64 && cell.x() < 0).orElseThrow());
    }

    @Test void noFreeSiteMeansNoPlacement() {
        var origin = new ConnectedColonyLayout.Cell(0, 64, 0);
        var colony = ConnectedColonyLayout.scan(origin, origin::equals, 8, 3);
        assertTrue(ConnectedColonyLayout.nextExtension(colony, cell -> false).isEmpty());
    }

    @Test void rejectsOversizedComponentsRatherThanPartiallyCounting() {
        var origin = new ConnectedColonyLayout.Cell(0, 64, 0);
        assertThrows(IllegalStateException.class,
            () -> ConnectedColonyLayout.scan(origin, c -> c.y() == 64, 2, 3));
    }
}
