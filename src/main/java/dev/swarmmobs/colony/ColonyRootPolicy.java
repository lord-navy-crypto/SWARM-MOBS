package dev.swarmmobs.colony;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/**
 * Colony membership and root-election decisions, without loading chunks.
 * A caller must provide a fully resolved, face-connected snapshot. If multiple
 * legacy active cores touch, do not silently merge their existing inventories.
 */
public final class ColonyRootPolicy {
    private ColonyRootPolicy() {}

    public record Core(ConnectedColonyLayout.Cell position,
                       boolean legacyActive, long resources) {
        public Core {
            Objects.requireNonNull(position);
            if (resources < 0) throw new IllegalArgumentException("negative resources");
        }
    }

    public record Election(ConnectedColonyLayout.Cell root,
                           int extensions, boolean conflict) {
        public Election {
            if (extensions < 0) throw new IllegalArgumentException("extensions");
        }
    }

    public static Election elect(Collection<Core> members) {
        Objects.requireNonNull(members);
        if (members.isEmpty()) throw new IllegalArgumentException("empty colony");
        long active = members.stream().filter(Core::legacyActive).count();
        if (active != 1) return new Election(null, 0, true);
        var root = members.stream().filter(Core::legacyActive)
                .map(Core::position).min(Comparator
                        .comparingInt(ConnectedColonyLayout.Cell::x)
                        .thenComparingInt(ConnectedColonyLayout.Cell::y)
                        .thenComparingInt(ConnectedColonyLayout.Cell::z)).orElseThrow();
        return new Election(root, members.size() - 1, false);
    }

    /** Extension blocks add capacity without an arbitrary maximum total size. */
    public static long totalCapacity(int legacyCoreCapacity, long extensions,
                                     int extensionCapacity) {
        if (legacyCoreCapacity < 1 || extensions < 0 || extensionCapacity < 1)
            throw new IllegalArgumentException("invalid capacity inputs");
        return Math.addExact(legacyCoreCapacity,
                Math.multiplyExact(extensions, (long) extensionCapacity));
    }
}
