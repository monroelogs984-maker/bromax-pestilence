package com.bromax.pestilence.item;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.disease.Disease;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A painful procedure that knocks one disease back a stage (from the first stage it cures).
 * LANCET: lances buboes; bubonic plague in the Buboes stage only. Reusable.
 * LEECHES: bloodletting; septicemic plague only. Used up (the jar is kept).
 * On anyone else it just hurts. Neither ever takes the patient below one heart.
 */
public class TreatmentItem extends Item {
    public enum Kind { LANCET, LEECHES }

    private final Kind kind;

    public TreatmentItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) perform(player, player, stack, hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!player.level().isClientSide) perform(player, target, stack, hand);
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private void perform(Player physician, LivingEntity patient, ItemStack stack, InteractionHand hand) {
        boolean sick = Plague.isSick(patient);
        int stage = sick ? patient.getEffect(ModRegistry.PLAGUE).getAmplifier() : -1;
        boolean works = switch (kind) {
            case LANCET -> sick && Plague.disease(patient) == Disease.BUBONIC && stage == Plague.MAX_STAGE;
            case LEECHES -> sick && Plague.disease(patient) == Disease.SEPTICEMIC;
        };
        float pain = kind == Kind.LANCET ? 2f : 3f;
        patient.hurt(patient.level().damageSources().generic(), Math.min(pain, Math.max(0, patient.getHealth() - 2f)));
        if (patient.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, patient.getX(), patient.getY(0.6), patient.getZ(), 4, 0.2, 0.2, 0.2, 0.1);
        }
        String key = "message." + Pestilence.MODID + "." + kind.name().toLowerCase();
        if (works) {
            Plague.setStage(patient, stage - 1);
            physician.displayClientMessage(Component.translatable(key + ".works").withStyle(ChatFormatting.DARK_GREEN), true);
        } else {
            physician.displayClientMessage(Component.translatable(key + ".useless").withStyle(ChatFormatting.GRAY), true);
        }
        SoundEvent sound = kind == Kind.LANCET ? net.minecraft.sounds.SoundEvents.PLAYER_HURT_SWEET_BERRY_BUSH : net.minecraft.sounds.SoundEvents.SLIME_SQUISH_SMALL;
        patient.level().playSound(null, patient.blockPosition(), sound, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 1f);
        if (kind == Kind.LANCET) {
            stack.hurtAndBreak(1, physician, LivingEntity.getSlotForHand(hand));
        } else if (!physician.getAbilities().instabuild) {
            stack.shrink(1);
            if (!physician.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) physician.drop(new ItemStack(Items.GLASS_BOTTLE), false);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
