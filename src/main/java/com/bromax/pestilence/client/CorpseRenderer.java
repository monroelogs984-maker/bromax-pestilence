package com.bromax.pestilence.client;

import com.bromax.pestilence.entity.Corpse;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Draws a corpse as the animal it was, lying on its side, by rendering a cached client-side
 * stand-in of the source type. While stirring (about to rise) it twitches.
 */
public class CorpseRenderer extends EntityRenderer<Corpse> {
    private final Map<EntityType<?>, Entity> standIns = new HashMap<>();
    private Level standInLevel;

    public CorpseRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5f;
    }

    @Override
    public void render(Corpse corpse, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        EntityType<?> type = corpse.getSource();
        if (type == null) return;
        if (standInLevel != corpse.level()) {
            standIns.clear();
            standInLevel = corpse.level();
        }
        if (!(standIns.computeIfAbsent(type, t -> t.create(corpse.level())) instanceof LivingEntity body)) return;

        // Facing north with no yaw of its own; the corpse's yaw is applied outside.
        body.yBodyRot = body.yBodyRotO = 180f;
        body.yHeadRot = body.yHeadRotO = 180f;
        body.setYRot(180f);
        body.setXRot(0f);

        pose.pushPose();
        pose.translate(0, body.getBbWidth() / 2, 0);
        pose.mulPose(Axis.YP.rotationDegrees(180f - corpse.getYRot()));
        pose.mulPose(Axis.ZP.rotationDegrees(90f));
        if (corpse.isStirring()) {
            float t = corpse.tickCount + partialTick;
            if (Mth.sin(t * 0.35f) > 0.6f) pose.mulPose(Axis.XP.rotationDegrees(Mth.sin(t * 3.1f) * 6f));
        }
        entityRenderDispatcher.getRenderer(body).render(body, 0f, partialTick, pose, buffers, light);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(Corpse corpse) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
