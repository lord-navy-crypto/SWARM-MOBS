package dev.swarmmobs.client.renderer;

import dev.swarmmobs.client.model.SwarmNestSignalModel;
import dev.swarmmobs.colony.SwarmNestBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Renders only the floating signal orb; the vanilla Nest Core block model is unchanged.
 */
public final class SwarmNestSignalRenderer extends GeoBlockRenderer<SwarmNestBlockEntity> {
    public SwarmNestSignalRenderer() {
        super(new SwarmNestSignalModel());
    }
}
