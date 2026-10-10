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

    /** A Form: drawn bigger as it grows. The Choking Shade is part fog, so it is drawn see-through. */
    public static class Form<T extends com.bromax.pestilence.entity.form.FormMob> extends GeoEntityRenderer<T> {
        private final float base;
        private final boolean fog;

        public Form(EntityRendererProvider.Context ctx, String model, float base) {
            super(ctx, new DefaultedEntityGeoModel<>(Pestilence.id(model), true));
            this.base = base;
            this.fog = model.equals("choking_shade");
        }

        @Override
        public void render(T form, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            float s = base * form.tierScale();
            withScale(s);
            shadowRadius = 0.6f * s;
            super.render(form, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public RenderType getRenderType(T animatable, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return fog ? RenderType.entityTranslucent(texture) : super.getRenderType(animatable, texture, buffers, partialTick);
        }
    }

    /** A Miasma Clot: see-through fog, drawn bigger with each thing it has taken in. */
    public static class Clot extends GeoEntityRenderer<com.bromax.pestilence.entity.form.MiasmaClot> {
        public Clot(EntityRendererProvider.Context ctx) {
            super(ctx, new DefaultedEntityGeoModel<>(Pestilence.id("miasma_clot"), false));
        }

        @Override
        public void render(com.bromax.pestilence.entity.form.MiasmaClot clot, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
            withScale(clot.sizeScale());
            shadowRadius = 0.4f * clot.sizeScale();
            super.render(clot, yaw, partialTick, pose, buffers, light);
        }

        @Override
        public RenderType getRenderType(com.bromax.pestilence.entity.form.MiasmaClot animatable, ResourceLocation texture, @Nullable MultiBufferSource buffers, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }
}
