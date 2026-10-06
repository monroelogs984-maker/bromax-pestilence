package com.bromax.pestilence.world;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.block.WitheredLeavesBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The Blight. It only ever touches grass blocks, natural (non-persistent) leaves, crops and small
 * plants, and everything it changes can change back: blighted soil becomes grass again, withered
 * leaves remember their species, and crops are only knocked back to seedlings.
 */
public final class Blight {
    private Blight() {}

    public static void spread(ServerLevel level, BlockPos center, int radius, int attempts) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < attempts; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            double dist = Math.sqrt(random.nextDouble()) * radius;
            int x = center.getX() + Mth.floor(Mth.cos(angle) * dist);
            int z = center.getZ() + Mth.floor(Mth.sin(angle) * dist);
            BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
            if (!level.isLoaded(top) || Tracker.inBrazier(level, top)) continue;
            blightAt(level, top);
        }
    }

    private static void blightAt(ServerLevel level, BlockPos top) {
        BlockState state = level.getBlockState(top);
        if (state.getBlock() instanceof LeavesBlock && !(state.getBlock() instanceof WitheredLeavesBlock)) {
            BlockState withered = WitheredLeavesBlock.witherFrom(state);
            if (withered != null) level.setBlock(top, withered, Block.UPDATE_CLIENTS);
            return;
        }
        if (state.getBlock() instanceof CropBlock crop) {
            if (crop.getAge(state) > 0) {
                level.setBlock(top, crop.getStateForAge(0), Block.UPDATE_ALL);
                level.sendParticles(ParticleTypes.SMOKE, top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.01);
            }
            return;
        }
        BlockPos ground = top;
        if (state.is(BlockTags.REPLACEABLE_BY_TREES) || state.is(BlockTags.FLOWERS)) {
            ground = top.below();
            if (level.getBlockState(ground).is(Blocks.GRASS_BLOCK)) level.destroyBlock(top, false);
        }
        if (level.getBlockState(ground).is(Blocks.GRASS_BLOCK)) {
            level.setBlock(ground, ModRegistry.BLIGHTED_SOIL.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Turn one blighted block back. Returns false if the block was not blighted. */
    public static boolean heal(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModRegistry.BLIGHTED_SOIL.get())) {
            level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            return true;
        }
        if (state.getBlock() instanceof WitheredLeavesBlock) {
            level.setBlock(pos, WitheredLeavesBlock.restore(state), Block.UPDATE_ALL);
            return true;
        }
        return false;
    }

    public static boolean isBlighted(BlockState state) {
        return state.is(ModRegistry.BLIGHTED_SOIL.get()) || state.getBlock() instanceof WitheredLeavesBlock;
    }
}
