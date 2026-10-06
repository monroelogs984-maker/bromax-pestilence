package com.bromax.pestilence.client;

import com.bromax.pestilence.block.ApothecaryTableBlockEntity;
import com.bromax.pestilence.block.ItemHolderBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/**
 * Shows what is on an Apothecary Table (base in the middle, ingredients at the corners, lying flat)
 * or hanging from a Drying Rack (four bundles along the top bar).
 */
public class ItemHolderRenderer<T extends ItemHolderBlockEntity> implements BlockEntityRenderer<T> {
    private static final double[][] TABLE_SPOTS = {{0.5, 0.5}, {0.25, 0.25}, {0.75, 0.25}, {0.25, 0.75}, {0.75, 0.75}};
    private final ItemRenderer items;

    public ItemHolderRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(T be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        boolean table = be instanceof ApothecaryTableBlockEntity;
        for (int i = 0; i < be.items().size(); i++) {
            ItemStack stack = be.items().get(i);
            if (stack.isEmpty()) continue;
            pose.pushPose();
            if (table) {
                pose.translate(TABLE_SPOTS[i][0], 1.01 + i * 0.002, TABLE_SPOTS[i][1]);
                pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + i * 23));
                pose.mulPose(Axis.XP.rotationDegrees(90));
                float scale = i == 0 ? 0.45f : 0.35f;
                pose.scale(scale, scale, scale);
            } else {
                pose.translate(0.5, 0, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
                pose.translate(-0.3 + i * 0.2, 0.62, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(180));   // hung head-down by the stems
                pose.scale(0.4f, 0.4f, 0.4f);
            }
            items.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong() + i);
            pose.popPose();
        }
    }
}
