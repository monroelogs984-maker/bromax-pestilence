package com.bromax.pestilence.item;

import com.bromax.pestilence.Pestilence;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * The Gravedigger's long-handled spade: a shovel that reaches a block and a half further, swings
 * like an axe, and buries the dead where they lie (see Corpse#interact).
 */
public class GravediggersSpadeItem extends ShovelItem {
    private static final ResourceLocation REACH = ResourceLocation.fromNamespaceAndPath(Pestilence.MODID, "spade_reach");

    public GravediggersSpadeItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(ShovelItem.createAttributes(Tiers.IRON, 4.5f, -3.0f)
                .withModifierAdded(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(REACH, 1.5, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .withModifierAdded(Attributes.BLOCK_INTERACTION_RANGE, new AttributeModifier(REACH, 1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
