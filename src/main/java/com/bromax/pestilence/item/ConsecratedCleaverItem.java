package com.bromax.pestilence.item;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.disease.Consecration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;

import java.util.List;

/**
 * Forged from the Gravewarden's steel and hallowed by deeds (see Consecration). Each level adds a
 * fifth more damage against the Pestilence's creatures; from level 3 it sets them alight; at
 * level 5 every one it fells mends its bearer.
 */
public class ConsecratedCleaverItem extends SwordItem {
    public static final SimpleTier SURGEONS_STEEL = new SimpleTier(BlockTags.INCORRECT_FOR_IRON_TOOL, 1200, 7f, 2.5f, 18,
            () -> Ingredient.of(ModRegistry.SURGEONS_STEEL.get()));

    public ConsecratedCleaverItem(Properties properties) {
        super(SURGEONS_STEEL, properties.attributes(SwordItem.createAttributes(SURGEONS_STEEL, 4, -2.6f)));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return Consecration.level(stack) >= 3 || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int level = Consecration.level(stack), next = Consecration.next(stack);
        tooltip.add(Component.translatable(getDescriptionId() + ".level", level).withStyle(ChatFormatting.GOLD));
        tooltip.add(next < 0 ? Component.translatable(getDescriptionId() + ".max").withStyle(ChatFormatting.GRAY)
                : Component.translatable(getDescriptionId() + ".next", Consecration.deeds(stack), next).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
