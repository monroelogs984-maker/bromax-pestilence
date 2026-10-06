package com.bromax.pestilence.item;

import com.bromax.pestilence.entity.ThrownBomb;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Quicklime or fumigation bomb: thrown like a snowball, see ThrownBomb for what each does. */
public class BombItem extends Item {
    public BombItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 0.6f, 0.6f);
        if (!level.isClientSide) {
            ThrownBomb bomb = new ThrownBomb(level, player);
            bomb.setItem(stack.copyWithCount(1));
            bomb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 1.2f, 1f);
            level.addFreshEntity(bomb);
        }
        player.getCooldowns().addCooldown(this, 20);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
