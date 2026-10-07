package com.bromax.pestilence.item;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.block.PyreBlock;
import com.bromax.pestilence.entity.boss.BlackBloom;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * The Pale Rider's heart. Bound in gold it becomes the Plaguebane Amulet; burned on a lit pyre at
 * night, at the height of the Pestilence, it wakes the Blackblight. That takes two tries: the
 * first only warns, and the second, within ten seconds, cannot be undone.
 */
public class HeartOfPestilenceItem extends Item {
    private static final String CONFIRM = Pestilence.MODID + ":blackblight_confirm";

    public HeartOfPestilenceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof PyreBlock) || !state.getValue(PyreBlock.LIT)) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
        Player player = context.getPlayer();
        PestilenceData data = PestilenceData.get(level.getServer());
        String refusal = !PestConfig.blackblightAllowed() ? "disabled"
                : data.phase() >= Phase.BLACKBLIGHT ? "already"
                : data.phase() < Phase.MAX ? "too_early"
                : !level.isNight() ? "day" : null;
        if (refusal != null) {
            player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".heart." + refusal).withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        long now = level.getGameTime();
        long asked = player.getPersistentData().getLong(CONFIRM);
        if (asked == 0 || now - asked > 200) {
            player.getPersistentData().putLong(CONFIRM, now);
            player.sendSystemMessage(Component.translatable("message." + Pestilence.MODID + ".heart.warning").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.translatable("message." + Pestilence.MODID + ".heart.warning2").withStyle(ChatFormatting.RED));
            level.playSound(null, context.getClickedPos(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.BLOCKS, 2f, 0.6f);
            return InteractionResult.CONSUME;
        }
        player.getPersistentData().remove(CONFIRM);
        context.getItemInHand().shrink(1);
        unleash(level, context.getClickedPos());
        return InteractionResult.CONSUME;
    }

    /** The Heart burns, the pyre goes out, and the Black Bloom rises in its place. */
    public static void unleash(ServerLevel level, BlockPos pyre) {
        level.setBlockAndUpdate(pyre, Blocks.AIR.defaultBlockState());
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pyre.getX() + 0.5, pyre.getY(), pyre.getZ() + 0.5);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        BlackBloom bloom = ModRegistry.BLACK_BLOOM.get().create(level);
        if (bloom != null) {
            bloom.moveTo(pyre.getX() + 0.5, pyre.getY(), pyre.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
            bloom.finalizeSpawn(level, level.getCurrentDifficultyAt(pyre), MobSpawnType.EVENT, null);
            level.addFreshEntity(bloom);
        }
        PestilenceData.get(level.getServer()).unleash(level.getServer());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
