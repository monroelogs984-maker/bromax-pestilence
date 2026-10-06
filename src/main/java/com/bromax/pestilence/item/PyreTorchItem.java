package com.bromax.pestilence.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A pitch-soaked brand. Sets whatever it strikes alight and burns corpses on contact. Wears out. */
public class PyreTorchItem extends SwordItem {
    public PyreTorchItem(Properties properties) {
        super(Tiers.WOOD, properties.attributes(SwordItem.createAttributes(Tiers.WOOD, 2, -2.2f)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        target.igniteForSeconds(4);
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide && holder instanceof Player p && (selected || p.getOffhandItem() == stack) && level.random.nextInt(3) == 0) {
            double side = p.getOffhandItem() == stack ? -0.35 : 0.35;
            double yaw = Math.toRadians(p.getYRot());
            level.addParticle(ParticleTypes.FLAME, p.getX() - Math.cos(yaw) * side, p.getY() + 1.2, p.getZ() - Math.sin(yaw) * side, 0, 0.02, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
