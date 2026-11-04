package dev.swarmmobs.mixin;

import dev.swarmmobs.config.SwarmConfig;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Applies the configured capacity to both vanilla's fullness query and any
 * inlined vanilla occupancy comparison, while retaining native bee storage.
 */
@Mixin(value = BeehiveBlockEntity.class, remap = false)
public abstract class BeehiveBlockEntityMixin {
    /**
     * @author Swarm Mobs
     * @reason Vanilla hive fullness must use the server-configured capacity.
     */
    @Overwrite(remap = false)
    public boolean isFull() {
        BeehiveBlockEntity hive = (BeehiveBlockEntity) (Object) this;
        return hive.getOccupantCount() >= SwarmConfig.BEE_HIVE_CAPACITY.get();
    }

    @ModifyConstant(method = "addOccupant", constant = @Constant(intValue = 3, ordinal = 0),
            require = 0, remap = false)
    private int swarmmobs$configuredAddOccupantLimit(int original) {
        return SwarmConfig.BEE_HIVE_CAPACITY.get();
    }
}
