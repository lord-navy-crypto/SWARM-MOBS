package dev.swarmmobs.colony;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Shared shape algorithm only: monster nests and vanilla beehives have separate
 * storage, behaviors, and controllers. Adjacency means six face-neighbors.
 * Input blocks must be in loaded, permitted positions; never force chunk loads.
 */
public final class ConnectedColonyLayout {
    private ConnectedColonyLayout() {}

    public record Cell(int x, int y, int z) {
        public List<Cell> neighbors() {
            return List.of(new Cell(x + 1, y, z), new Cell(x - 1, y, z),
                    new Cell(x, y + 1, z), new Cell(x, y - 1, z),
                    new Cell(x, y, z + 1), new Cell(x, y, z - 1));
        }
    }

    public record Colony(Set<Cell> cells, int capacity) {
        public Colony {
            cells = Set.copyOf(cells);
            if (capacity < 0) throw new IllegalArgumentException("negative capacity");
        }
    }

    /**
     * Connected component bounded by maxBlocks and maxVisits. A maxBlocks hit
     * fails closed rather than silently counting a partial colony.
     */
    public static Colony scan(Cell origin, Predicate<Cell> loadedMember,
                              int maxBlocks, int capacityPerBlock) {
        Objects.requireNonNull(origin);
        Objects.requireNonNull(loadedMember);
        if (maxBlocks < 1 || maxBlocks > 4096 || capacityPerBlock < 1 ||
                (long) maxBlocks * capacityPerBlock > Integer.MAX_VALUE)
            throw new IllegalArgumentException("invalid limits");
        if (!loadedMember.test(origin)) return new Colony(Set.of(), 0);
        Set<Cell> visited = new HashSet<>();
        Set<Cell> members = new HashSet<>();
        ArrayDeque<Cell> pending = new ArrayDeque<>();
        pending.add(origin);
        // Six potential neighbors per accepted block; strict scan budget.
        int checks = 0;
        int maxChecks = maxBlocks * 6 + 1;
        while (!pending.isEmpty()) {
            Cell cell = pending.removeFirst();
            if (!visited.add(cell)) continue;
            if (++checks > maxChecks)
                throw new IllegalStateException("colony scan budget exceeded");
            if (!loadedMember.test(cell)) continue;
            if (members.size() >= maxBlocks)
                throw new IllegalStateException("colony exceeds block limit");
            members.add(cell);
            for (Cell candidate : cell.neighbors()) {
                if (!visited.contains(candidate)) pending.addLast(candidate);
            }
        }
        return new Colony(members, members.size() * capacityPerBlock);
    }

    /**
     * Find stable nearby placements. The caller must recheck safety and
     * deduct costs atomically on the server before placing the existing block.
     */
    public static Optional<Cell> nextExtension(Colony colony,
                                               Predicate<Cell> validAirSite) {
        Objects.requireNonNull(colony);
        Objects.requireNonNull(validAirSite);
        Set<Cell> candidates = new HashSet<>();
        for (Cell member : colony.cells()) {
            for (Cell neighbor : member.neighbors()) {
                if (!colony.cells().contains(neighbor)) candidates.add(neighbor);
            }
        }
        Comparator<Cell> deterministic = Comparator.comparingInt(Cell::y)
                .thenComparingInt(Cell::x).thenComparingInt(Cell::z);
        return candidates.stream().sorted(deterministic).filter(validAirSite).findFirst();
    }
}
