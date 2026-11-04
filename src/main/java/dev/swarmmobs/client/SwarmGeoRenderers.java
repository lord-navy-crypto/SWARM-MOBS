package dev.swarmmobs.client;

import dev.swarmmobs.SwarmMobs;
import dev.swarmmobs.client.renderer.SwarmNestSignalRenderer;
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

    private SwarmGeoRenderers() {}
}
