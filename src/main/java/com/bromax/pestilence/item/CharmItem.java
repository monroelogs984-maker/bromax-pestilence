package com.bromax.pestilence.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.fml.ModList;

import java.util.List;

/** Pomander or Plaguebane Amulet. Worn in a Curios slot, or without Curios held in the offhand (the amulet: carried anywhere). */
public class CharmItem extends Item {
    private final boolean foil;

    public CharmItem(Properties properties, boolean foil) {
        super(properties);
        this.foil = foil;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return foil || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.bromaxpestilence.charm." + (ModList.get().isLoaded("curios") ? "curios" : "plain"))
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
