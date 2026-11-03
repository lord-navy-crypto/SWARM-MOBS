package dev.swarmmobs;

import dev.swarmmobs.ai.ollama.OllamaStrategyProvider;
import dev.swarmmobs.bee.BeeHiveEvents;
import dev.swarmmobs.bee.BeeColonyEvents;
import dev.swarmmobs.bee.BeeFlowerEvents;
import dev.swarmmobs.bee.BeeEcologyEvents;
import dev.swarmmobs.command.SwarmCommands;
import dev.swarmmobs.config.SwarmConfig;
import dev.swarmmobs.data.SwarmAttachments;
import dev.swarmmobs.event.SwarmMobEvents;
import dev.swarmmobs.event.SwarmPlayerStyleEvents;
import dev.swarmmobs.gametest.SwarmGameTestRegistration;
import dev.swarmmobs.network.SwarmControlNetwork;
import dev.swarmmobs.registry.SwarmNestBlocks;
import dev.swarmmobs.registry.SwarmNestBlockEntities;
import dev.swarmmobs.registry.SwarmNestItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SwarmMobs.MOD_ID)
public final class SwarmMobs {
    public static final String MOD_ID = "swarmmobs";
    public static final Logger LOGGER = LogUtils.getLogger();


    private void onServerStarted(ServerStartedEvent event) {
        // This is a one-shot, non-blocking probe. A missing local Ollama process
        // is informational only and never changes deterministic gameplay.
        OllamaStrategyProvider.INSTANCE.discover().whenComplete((discovery, error) -> {
            if (error != null) {
                LOGGER.info(
                        "Optional local Ollama discovery unavailable at startup: {}. "
                                + "Retry with /swarmmobs ai discover; deterministic swarm behavior remains active.",
                        conciseError(error)
                );
                return;
            }

            String installed = discovery.models().stream()
                    .limit(8)
                    .map(model -> model.name())
                    .collect(java.util.stream.Collectors.joining(", "));
            if (installed.isBlank()) {
                installed = "(none)";
            }
            LOGGER.info(
                    "Local Ollama discovery complete: version={}, installedModels={} ({} total).",
                    discovery.version(),
                    installed,
                    discovery.models().size()
            );
        });
    }

    private static String conciseError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        String message = cause.getMessage();
        return cause.getClass().getSimpleName()
                + (message == null || message.isBlank() ? "" : ": " + message);
    }

    public SwarmMobs(IEventBus modBus, ModContainer modContainer) {
        SwarmAttachments.ATTACHMENT_TYPES.register(modBus);
        SwarmNestBlocks.BLOCKS.register(modBus);
        SwarmNestBlockEntities.TYPES.register(modBus);
        SwarmNestItems.ITEMS.register(modBus);
        modBus.addListener(SwarmGameTestRegistration::register);
        modBus.addListener(SwarmControlNetwork::register);
        modContainer.registerConfig(ModConfig.Type.SERVER, SwarmConfig.SPEC);

        NeoForge.EVENT_BUS.addListener(SwarmMobEvents::onEntityJoin);
        NeoForge.EVENT_BUS.addListener(SwarmMobEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(SwarmPlayerStyleEvents::onLivingDamagePost);
        // Observe completed Bee trips and assign worker priorities before
        // the shelter and hive-growth adapters consume the same tick's state.
        NeoForge.EVENT_BUS.addListener(BeeEcologyEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(BeeFlowerEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(BeeColonyEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(BeeHiveEvents::onEntityTick);
        NeoForge.EVENT_BUS.addListener(SwarmCommands::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);

        LOGGER.info("Swarm Mobs {} initialized: deterministic swarm control active; local Ollama AI Shadow Mode available and OFF by default.",
                modContainer.getModInfo().getVersion());
    }
}
