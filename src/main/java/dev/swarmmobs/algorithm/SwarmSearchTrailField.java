package dev.swarmmobs.algorithm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Bounded, short-lived memory of locations actually visited during target search. */
public final class SwarmSearchTrailField {
    public static final int CELL_SIZE = 4;
    public static final int MAX_CELLS = 384;
    public static final long HALF_LIFE_TICKS = 360L;
    public static final long MAX_AGE_TICKS = 2400L;
    private static final double MAX_STRENGTH = 4.0;
    private static final double FORGET_THRESHOLD = 0.025;

    public record Position(int x, int y, int z) {}
    private record CellKey(UUID targetId, int x, int y, int z) {
        private CellKey offset(int dx, int dz) {
            return new CellKey(targetId, x + dx, y, z + dz);
        }
    }

    private static final class Cell {
        private double strength;
        private long updatedAt;
        private long lastMarkedAt;
        private Cell(long now) {
            updatedAt = now;
            lastMarkedAt = now;
        }
        private void decay(long now) {
            if (now <= updatedAt) return;
            strength *= Math.pow(0.5, (now - updatedAt) / (double) HALF_LIFE_TICKS);
            updatedAt = now;
        }
    }

    private final Map<CellKey, Cell> cells = new HashMap<>();

    public boolean mark(UUID targetId, Position position, long now) {
        if (targetId == null || position == null || now < 0) return false;
        prune(now);
        CellKey key = key(targetId, position);
        Cell cell = cells.get(key);
        if (cell == null && cells.size() >= MAX_CELLS) evictWeakest(now);
        cell = cells.computeIfAbsent(key, ignored -> new Cell(now));
        if (now < cell.lastMarkedAt) return false;
        cell.decay(now);
        cell.strength = Math.min(MAX_STRENGTH, cell.strength + 1.0);
        cell.lastMarkedAt = now;
        return true;
    }

    public double coverage(UUID targetId, Position position, long now) {
        if (targetId == null || position == null || now < 0) return 0.0;
        CellKey center = key(targetId, position);
        return read(center, now) + 0.18 * (
                read(center.offset(1, 0), now) + read(center.offset(-1, 0), now)
                + read(center.offset(0, 1), now) + read(center.offset(0, -1), now));
    }

    public int size(long now) {
        if (now >= 0) prune(now);
        return cells.size();
    }

    private static CellKey key(UUID targetId, Position position) {
        return new CellKey(targetId, Math.floorDiv(position.x(), CELL_SIZE),
                Math.floorDiv(position.y(), CELL_SIZE * 2),
                Math.floorDiv(position.z(), CELL_SIZE));
    }

    private double read(CellKey key, long now) {
        Cell cell = cells.get(key);
        if (cell == null || now < cell.updatedAt) return 0.0;
        cell.decay(now);
        if (now - cell.lastMarkedAt >= MAX_AGE_TICKS
                || cell.strength < FORGET_THRESHOLD) {
            cells.remove(key);
            return 0.0;
        }
        return cell.strength;
    }

    private void prune(long now) {
        Iterator<Map.Entry<CellKey, Cell>> it = cells.entrySet().iterator();
        while (it.hasNext()) {
            Cell cell = it.next().getValue();
            if (now < cell.updatedAt) continue;
            cell.decay(now);
            if (now - cell.lastMarkedAt >= MAX_AGE_TICKS
                    || cell.strength < FORGET_THRESHOLD) it.remove();
        }
    }

    private void evictWeakest(long now) {
        CellKey weakest = null;
        double lowest = Double.POSITIVE_INFINITY;
        for (var entry : cells.entrySet()) {
            Cell cell = entry.getValue();
            cell.decay(now);
            if (cell.strength < lowest) {
                weakest = entry.getKey();
                lowest = cell.strength;
            }
        }
        if (weakest != null) cells.remove(weakest);
    }
}