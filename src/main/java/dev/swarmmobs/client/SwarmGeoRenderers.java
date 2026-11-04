package dev.swarmmobs.client;

import dev.swarmmobs.SwarmMobs;
import dev.swarmmobs.client.renderer.SwarmNestSignalRenderer;
import dev.swarmmobs.client.renderer.SwarmActionRenderLayer;
import dev.swarmmobs.visual.SwarmActionAnimatable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import dev.swarmmobs.registry.SwarmNestBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * Client-only GeckoLib renderer registration.
 */
@EventBusSubscriber(modid = SwarmMobs.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SwarmGeoRenderers {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                SwarmNestBlockEntities.NEST.get(),
                context -> new SwarmNestSignalRenderer()
        );
    }

    @SubscribeEvent
    public static void addActionLayers(EntityRenderersEvent.AddLayers event) {
        addActionLayer(event, EntityType.BEE, SwarmActionAnimatable.BEE);
        addActionLayer(event, EntityType.ZOMBIE, SwarmActionAnimatable.ZOMBIE);
        addActionLayer(event, EntityType.SPIDER, SwarmActionAnimatable.SPIDER);
        addActionLayer(event, EntityType.SKELETON, SwarmActionAnimatable.SKELETON);
        addActionLayer(event, EntityType.CREEPER, SwarmActionAnimatable.CREEPER);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T extends LivingEntity> void addActionLayer(
            EntityRenderersEvent.AddLayers event,
            EntityType<T> type,
            SwarmActionAnimatable animatable
    ) {
        EntityRenderer<T> base = event.getRenderer(type);
        if (base instanceof LivingEntityRenderer living) {
            LivingEntityRenderer<T, EntityModel<T>> renderer =
                    (LivingEntityRenderer<T, EntityModel<T>>) living;
            renderer.addLayer(new SwarmActionRenderLayer<>(
                    renderer, event.getContext(), animatable));
        }
    }

    private SwarmGeoRenderers() {}
}
