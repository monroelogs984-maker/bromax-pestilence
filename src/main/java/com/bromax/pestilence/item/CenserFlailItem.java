package com.bromax.pestilence.item;

import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.entity.vermin.VerminCloud;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A smoking censer on a chain: slow, heavy blows, each one a burst of holy smoke that clears
 * miasma and vermin clouds around whatever it strikes. It hurts Miasma Wraiths double.
 */
public class CenserFlailItem extends SwordItem {
    public static final double CLEAR_RADIUS = 3.0;

    public CenserFlailItem(Properties properties) {
        super(Tiers.IRON, properties.attributes(SwordItem.createAttributes(Tiers.IRON, 7, -3.3f)));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target.level() instanceof ServerLevel level) {
            var area = target.getBoundingBox().inflate(CLEAR_RADIUS);
            level.getEntitiesOfClass(MiasmaCloud.class, area).forEach(MiasmaCloud::discard);
            level.getEntitiesOfClass(VerminCloud.class, area).forEach(cloud -> cloud.hurt(level.damageSources().mobAttack(attacker), 1000f));
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.getX(), target.getY(0.6), target.getZ(), 8, 0.6, 0.4, 0.6, 0.01);
            level.playSound(null, target.blockPosition(), SoundEvents.CHAIN_HIT, SoundSource.PLAYERS, 1f, 0.7f);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
