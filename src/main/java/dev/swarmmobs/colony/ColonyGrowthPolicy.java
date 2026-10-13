package dev.swarmmobs.colony;

/** Pure finite nest-growth gate; the world adapter owns actual block placement. */
public final class ColonyGrowthPolicy {
    private ColonyGrowthPolicy() {}

    public record State(int members, int capacity, int storedResources,
                        int resourceCapacity, int freeAdjacentSites,
                        int existingExtensions, int maximumExtensions) {
        public State {
            if (members < 0 || capacity < 1 || storedResources < 0 || resourceCapacity < 1
                    || storedResources > resourceCapacity || freeAdjacentSites < 0
                    || existingExtensions < 0 || maximumExtensions < 0
                    || existingExtensions > maximumExtensions)
                throw new IllegalArgumentException("invalid colony growth state");
        }
    }

    public static boolean canExpand(State state, int resourceCost) {
        if (state == null || resourceCost <= 0) throw new IllegalArgumentException("invalid cost");
        return state.members >= state.capacity()
                && state.storedResources() == state.resourceCapacity()
                && state.storedResources() >= resourceCost
                && state.freeAdjacentSites() > 0
                && state.existingExtensions() < state.maximumExtensions();
    }

    public static int capacityAfterExtension(int oldCapacity, int spacesPerExtension) {
        if (oldCapacity < 1 || spacesPerExtension < 1 ||
                oldCapacity > Integer.MAX_VALUE - spacesPerExtension)
            throw new IllegalArgumentException("invalid capacity");
        return oldCapacity + spacesPerExtension;
    }
}
