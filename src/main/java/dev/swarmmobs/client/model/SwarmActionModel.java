package dev.swarmmobs.client.model;

import dev.swarmmobs.SwarmMobs;
import dev.swarmmobs.visual.SwarmActionAnimatable;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** Small floating action glyph; vanilla mob geometry and equipment stay untouched. */
public final class SwarmActionModel extends GeoModel<SwarmActionAnimatable> {
    @Override
    public ResourceLocation getModelResource(SwarmActionAnimatable animatable) {
        return ResourceLocation.fromNamespaceAndPath(SwarmMobs.MOD_ID, "geo/swarm_action.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(SwarmActionAnimatable animatable) {
        return ResourceLocation.withDefaultNamespace("textures/block/amethyst_block.png");
    }

    @Override
    public ResourceLocation getAnimationResource(SwarmActionAnimatable animatable) {
        return ResourceLocation.fromNamespaceAndPath(SwarmMobs.MOD_ID, "animations/swarm_action.animation.json");
    }
}
