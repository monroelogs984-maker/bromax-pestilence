package com.bromax.pestilence.client;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.item.PlagueMaskItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.DefaultedItemGeoModel;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.function.Function;

/** Draws worn curios on the player. Only loaded when Curios is installed (see ClientSetup). */
final class CurioRenderers {
    private CurioRenderers() {}

    static void register() {
        CuriosRendererRegistry.register(ModRegistry.PLAGUE_MASK.get(), MaskRenderer::new);
        // the necklaces hang on the chest; the pomander can also be a charm, worn the same way
        CuriosRendererRegistry.register(ModRegistry.PLAGUEBANE_AMULET.get(), () -> new SpriteRenderer(m -> m.body, 0, 3.5f, -2.3f, 0, 0.32f));
        CuriosRendererRegistry.register(ModRegistry.POMANDER.get(), () -> new SpriteRenderer(m -> m.body, 0, 4.5f, -2.5f, 0, 0.3f));
        // the monocle over the right eye, the bracelet round the left wrist, the censer at the right hip
        CuriosRendererRegistry.register(ModRegistry.PHYSICIANS_MONOCLE.get(), () -> new SpriteRenderer(m -> m.head, -2f, -4f, -4.3f, 0, 0.22f));
        CuriosRendererRegistry.register(ModRegistry.RAT_TAIL_BRACELET.get(), () -> new SpriteRenderer(m -> m.leftArm, 3.1f, 7.5f, 0, 90, 0.3f));
        CuriosRendererRegistry.register(ModRegistry.CENSER.get(), () -> new SpriteRenderer(m -> m.body, -3f, 11.5f, -2.4f, 0, 0.38f));
    }

    @SuppressWarnings("unchecked")
    private static HumanoidModel<LivingEntity> humanoid(RenderLayerParent<?, ?> parent) {
        EntityModel<?> model = parent.getModel();
        return model instanceof HumanoidModel<?> h ? (HumanoidModel<LivingEntity>) h : null;
    }

    /** The beaked mask from a head slot: the same GeckoLib model as when worn as a helmet. */
    private static final class MaskRenderer implements ICurioRenderer {
        private final GeoArmorRenderer<PlagueMaskItem> renderer = new GeoArmorRenderer<>(new DefaultedItemGeoModel<>(Pestilence.id("armor/plague_mask")));

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose, RenderLayerParent<T, M> parent,
                MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            HumanoidModel<LivingEntity> base = humanoid(parent);
            if (base == null || !slot.visible()) return;
            renderer.prepForRender(slot.entity(), stack, EquipmentSlot.HEAD, base, buffers, partialTicks, limbSwing, limbSwingAmount, netHeadYaw, headPitch);
            base.copyPropertiesTo((HumanoidModel<LivingEntity>) (HumanoidModel<?>) renderer);
            renderer.renderToBuffer(pose, null, light, OverlayTexture.NO_OVERLAY, 0xffffffff);
        }
    }

    /** A worn trinket drawn as its own item sprite, pinned to a body part (offsets in model pixels). */
    private static final class SpriteRenderer implements ICurioRenderer {
        private final Function<HumanoidModel<LivingEntity>, ModelPart> part;
        private final float x, y, z, yaw, scale;

        SpriteRenderer(Function<HumanoidModel<LivingEntity>, ModelPart> part, float x, float y, float z, float yaw, float scale) {
            this.part = part;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.scale = scale;
        }

        @Override
        public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose, RenderLayerParent<T, M> parent,
                MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
            HumanoidModel<LivingEntity> model = humanoid(parent);
            if (model == null || !slot.visible()) return;
            pose.pushPose();
            part.apply(model).translateAndRotate(pose);
            pose.translate(x / 16f, y / 16f, z / 16f);
            pose.mulPose(Axis.YP.rotationDegrees(yaw));
            // model space is upside down and faces -Z; a half turn about X sets the sprite upright, facing out
            pose.mulPose(Axis.XP.rotationDegrees(180));
            pose.scale(scale, scale, scale);
            Minecraft.getInstance().getItemRenderer().renderStatic(slot.entity(), stack, ItemDisplayContext.FIXED, false, pose, buffers,
                    slot.entity().level(), light, OverlayTexture.NO_OVERLAY, slot.entity().getId());
            pose.popPose();
        }
    }

}
