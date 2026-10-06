package com.bromax.pestilence.item;

import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.afflicted.AfflictedMob;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A cure for particular diseases (Theriac: all of them). Taken yourself, or used on a sick villager
 * or animal. The wrong remedy is used up and does nothing, so diagnose first.
 */
public class RemedyItem extends Item {
    private final Set<Disease> cures;
    private final UseAnim anim;
    @Nullable
    private final ItemLike container;

    public RemedyItem(Properties properties, Set<Disease> cures, UseAnim anim, @Nullable ItemLike container) {
        super(properties);
        this.cures = cures.isEmpty() ? EnumSet.allOf(Disease.class) : EnumSet.copyOf(cures);
        this.anim = anim;
        this.container = container;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        if (!level.isClientSide) apply(entity, entity instanceof Player p ? p : null);
        if (entity instanceof Player player && player.getAbilities().instabuild) return stack;
        stack.shrink(1);
        if (container == null) return stack;
        if (stack.isEmpty()) return new ItemStack(container);
        if (entity instanceof Player player && !player.getInventory().add(new ItemStack(container))) {
            player.drop(new ItemStack(container), false);
        }
        return stack;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof AfflictedMob || !Plague.isSick(target)) return InteractionResult.PASS;
        if (player.level() instanceof ServerLevel level) {
            apply(target, player);
            level.playSound(null, target.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 1f, 1f);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (container != null && !player.getInventory().add(new ItemStack(container))) player.drop(new ItemStack(container), false);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    private void apply(LivingEntity patient, @Nullable Player physician) {
        boolean wasSick = Plague.isSick(patient);
        if (Plague.treat(patient, cures)) {
            if (patient.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, patient.getX(), patient.getEyeY(), patient.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
            }
            if (physician != null) physician.displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".cured")
                    .withStyle(ChatFormatting.DARK_GREEN), true);
        } else if (physician != null) {
            physician.displayClientMessage(Component.translatable("message." + Pestilence.MODID + (wasSick ? ".wrong_remedy" : ".not_sick"))
                    .withStyle(ChatFormatting.GRAY), true);
        }
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return anim;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}
