package dev.swarmmobs.mixin;

import dev.swarmmobs.config.SwarmConfig;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Applies the server-configured occupancy limit while retaining vanilla hive
 * storage, NBT, release behavior, honey state, and block entity implementation.
 */
@Mixin(value = BeehiveBlockEntity.class, remap = false)
public abstract class BeehiveBlockEntityMixin {
    @Inject(method = "isFull", at = @At("HEAD"), cancellable = true, remap = false)
    private void swarmmobs$useConfiguredCapacity(CallbackInfoReturnable<Boolean> cir) {
        BeehiveBlockEntity hive = (BeehiveBlockEntity) (Object) this;
        cir.setReturnValue(hive.getOccupantCount() >= SwarmConfig.BEE_HIVE_CAPACITY.get());
    }
}
