package dev.swarmmobs.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.swarmmobs.client.model.SwarmActionModel;
import dev.swarmmobs.visual.SwarmActionAnimatable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.renderer.GeoReplacedEntityRenderer;

/**
 * GeckoLib overlay attached to the existing vanilla mob renderer.
 * It only renders the small action glyph and never replaces the vanilla body.
 */
public final class SwarmActionRenderLayer<T extends LivingEntity>
        extends RenderLayer<T, EntityModel<T>> {
    private final GeoReplacedEntityRenderer<T, SwarmActionAnimatable> actionRenderer;

    public SwarmActionRenderLayer(
            RenderLayerParent<T, EntityModel<T>> parent,
            EntityRendererProvider.Context context,
            SwarmActionAnimatable animatable
    ) {
        super(parent);
        this.actionRenderer = new GeoReplacedEntityRenderer<>(
                context, new SwarmActionModel(), animatable);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!entity.isAlive() || entity.isInvisible()) return;

        poseStack.pushPose();
        // Restore entity-local coordinates from the vanilla model-layer frame.
        poseStack.translate(0.0, 1.501, 0.0);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(bodyYaw - 180.0F));
        poseStack.translate(0.0, entity.getBbHeight() + 0.12, 0.0);

        actionRenderer.render(entity, 0.0F, partialTick,
                poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
