package dev.swarmmobs.colony;

import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ColonyExpansionPlannerTest {
    @Test void picksFaceConnectedAirOnly() {
        var base = new ConnectedColonyLayout.Cell(0, 64, 0);
        var cells = Set.of(base);
        var result = ColonyExpansionPlanner.choose(
                cells, c -> c.y() == 64 && c.x() >= 0,
                ColonyExpansionPlanner.Shape.HORIZONTAL, 42L, 20);
        assertTrue(result.isPresent());
        assertTrue(base.neighbors().contains(result.orElseThrow()));
    }

    @Test void rejectsBlockedOrUnloadedWorldCandidates() {
        var cells = Set.of(new ConnectedColonyLayout.Cell(0, 64, 0));
        assertTrue(ColonyExpansionPlanner.choose(cells, c -> false,
                ColonyExpansionPlanner.Shape.COMPACT, 17L, 6).isEmpty());
    }

    @Test void reproducibleWithIdenticalSeedAndWorld() {
        var cells = Set.of(new ConnectedColonyLayout.Cell(0, 64, 0),
                new ConnectedColonyLayout.Cell(1, 64, 0));
        var first = ColonyExpansionPlanner.choose(cells, c -> true,
                ColonyExpansionPlanner.Shape.COMPACT, 73, 20);
        var second = ColonyExpansionPlanner.choose(cells, c -> true,
                ColonyExpansionPlanner.Shape.COMPACT, 73, 20);
        assertEquals(first, second);
    }

    @Test void doesNotRequireMaximumColonyPopulation() {
        var cells = new java.util.HashSet<ConnectedColonyLayout.Cell>();
        for (int x = 0; x < 1000; x++) {
            cells.add(new ConnectedColonyLayout.Cell(x, 64, 0));
        }
        assertTrue(ColonyExpansionPlanner.choose(cells,
                c -> c.y() == 64 && c.z() == 1,
                ColonyExpansionPlanner.Shape.HORIZONTAL, 123, 10000).isPresent());
    }
}
