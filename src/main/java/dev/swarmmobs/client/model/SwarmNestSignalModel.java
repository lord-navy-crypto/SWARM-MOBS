package dev.swarmmobs.client.model;

import dev.swarmmobs.SwarmMobs;
import dev.swarmmobs.colony.SwarmNestBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * Animated signal orb rendered above the existing Nest Core block model.
 */
public final class SwarmNestSignalModel extends GeoModel<SwarmNestBlockEntity> {
    @Override
    public ResourceLocation getModelResource(SwarmNestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(SwarmMobs.MOD_ID, "geo/nest_signal.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwarmNestBlockEntity animatable) {
        return ResourceLocation.withDefaultNamespace("textures/block/amethyst_block.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SwarmNestBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(SwarmMobs.MOD_ID, "animations/nest_signal.animation.json");
    }
}
