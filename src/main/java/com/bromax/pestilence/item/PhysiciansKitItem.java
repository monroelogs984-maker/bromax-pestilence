package com.bromax.pestilence.item;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.disease.Ailment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Use on a villager or animal, or on yourself (use in the air), to name the sickness and its stage. */
public class PhysiciansKitItem extends Item {
    public PhysiciansKitItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) examine(player, player, stack, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide) examine(player, target, stack, hand);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private static void examine(Player physician, LivingEntity patient, ItemStack kit, InteractionHand hand) {
        String key = "message." + Pestilence.MODID + ".diagnosis";
        Component who = patient == physician ? Component.translatable(key + ".self") : patient.getDisplayName();
        if (Plague.isSick(patient)) {
            Ailment ailment = Plague.ailment(patient);
            patient.setData(ModRegistry.AILMENT, ailment.diagnose());
            int stage = patient.getEffect(ModRegistry.PLAGUE).getAmplifier();
            physician.sendSystemMessage(Component.translatable(key, who, ailment.disease().displayName().copy().withStyle(ChatFormatting.DARK_RED),
                    Component.translatable(key + ".stage" + stage)).withStyle(ChatFormatting.GOLD));
            physician.sendSystemMessage(Component.translatable("disease." + Pestilence.MODID + "." + ailment.disease().getSerializedName() + ".treat")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else {
            MobEffectInstance immunity = patient.getEffect(ModRegistry.IMMUNITY);
            physician.sendSystemMessage((immunity != null
                    ? Component.translatable(key + ".immune", who, immunity.getDuration() / 1200 + 1)
                    : Component.translatable(key + ".healthy", who)).withStyle(ChatFormatting.GREEN));
        }
        physician.level().playSound(null, physician.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        kit.hurtAndBreak(1, physician, LivingEntity.getSlotForHand(hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
