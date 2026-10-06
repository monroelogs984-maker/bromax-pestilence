package com.bromax.pestilence.client;

import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.entity.herald.MiasmaWraith;
import com.bromax.pestilence.entity.vermin.BurrowerRat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** The two plague mobs that need more than a plain GeckoLib renderer. */
public final class PlagueRenderers {
    private PlagueRenderers() {}

    /** Drawn see-through: it is mostly foul air. */
    public static class Wraith extends GeoEntityRenderer<MiasmaWraith> {
        public Wraith(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(Pestilence.id("miasma_wraith"), true));
        }

        @Override
        public RenderType getRenderType(MiasmaWraith animatable, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }

    /** Not drawn at all while dug in; only its trickle of earth gives it away. */
    public static class Burrower extends GeoEntityRenderer<BurrowerRat> {
        public Burrower(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(Pestilence.id("burrower_rat"), true));
        }

        @Override
        public void render(BurrowerRat rat, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            if (!rat.isBurrowed()) super.render(rat, yaw, partialTick, pose, buffers, light);
        }
    }
}
