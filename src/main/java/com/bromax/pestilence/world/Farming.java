package com.bromax.pestilence.world;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.entity.PlagueRat;
import com.bromax.pestilence.phase.Phase;
import com.bromax.pestilence.phase.PestilenceData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Farming feeds the Pestilence: every harvest swells it, and fields and granaries draw rats. */
public final class Farming {
    private static final int RADIUS = 8;

    private Farming() {}

    /** A mature crop broken by a player (not in creative). */
    public static void harvested(ServerLevel level, ServerPlayer player, BlockState state) {
        if (!PestConfig.FARMING.get() || player.isCreative()) return;
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) return;
        if (PestilenceData.phase(level) < Phase.FULL_RATS) return;
        Plague.addPoints(level, PestConfig.POINTS_HARVEST.get());
    }

    /**
     * Rolled per player: a big enough farm around them (farmland, with hay bales and composters
     * counting as stores) draws rats at night, or at any hour to a proper granary. Each rat eats
     * the crop it comes out of.
     */
    public static void roll(ServerLevel level, ServerPlayer player) {
        if (!PestConfig.FARMING.get() || PestilenceData.phase(level) < Phase.FULL_RATS) return;
        BlockPos centre = player.blockPosition();
        int farmland = 0, stores = 0;
        List<BlockPos> crops = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-RADIUS, -3, -RADIUS), centre.offset(RADIUS, 3, RADIUS))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FARMLAND)) farmland++;
            else if (state.is(Blocks.HAY_BLOCK) || state.is(Blocks.COMPOSTER)) stores++;
            else if (state.getBlock() instanceof CropBlock) crops.add(pos.immutable());
        }
        int size = farmland + stores * 4;
        boolean granary = stores >= 4;
        if (size < PestConfig.FARM_SIZE.get() || crops.isEmpty() || !(level.isNight() || granary)) return;
        double chance = PestConfig.FARM_RAT_CHANCE.get() * Math.min(2.0, size / (double) PestConfig.FARM_SIZE.get());
        if (level.random.nextDouble() >= chance) return;
        int count = 1 + level.random.nextInt(Math.min(3, 1 + size / 32));
        int nearby = level.getEntitiesOfClass(PlagueRat.class, player.getBoundingBox().inflate(24)).size();
        for (int i = 0; i < count && nearby + i < 8; i++) {
            BlockPos at = crops.get(level.random.nextInt(crops.size()));
            PlagueRat rat = ModRegistry.PLAGUE_RAT.get().create(level);
            if (rat == null) continue;
            rat.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
            rat.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
            level.addFreshEntity(rat);
            BlockState crop = level.getBlockState(at);
            if (crop.getBlock() instanceof CropBlock block) level.setBlockAndUpdate(at, block.getStateForAge(0));
            level.playSound(null, at, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1f, 0.8f);
        }
        player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".farm_rats").withStyle(ChatFormatting.DARK_GREEN), true);
    }
}
