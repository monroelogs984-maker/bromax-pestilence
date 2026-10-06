package com.bromax.pestilence.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A surgeon's saw: quick and light in a fight, and it takes the dead apart for bone and ichor
 * (see Corpse#interact), at some risk to whoever does the sawing.
 */
public class BoneSawItem extends SwordItem {
    /** Chance of catching something from the body, scaled down by the Doctor's Garb. */
    public static final double RISK = 0.25;

    public BoneSawItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(SwordItem.createAttributes(Tiers.IRON, 1, -1.4f)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
