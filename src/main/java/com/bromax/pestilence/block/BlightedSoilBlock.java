package com.bromax.pestilence.block;

import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.world.Blight;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grass the Blight has reached. Nothing can be planted in it (it is not dirt), it gives off spores,
 * and lingering on it unmasked may make you sick. With no nest nearby it turns back into grass.
 */
public class BlightedSoilBlock extends Block {
    public BlightedSoilBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 && !Tracker.nestWithin(level, pos, PestConfig.BLIGHT_HEAL_RANGE.get())) Blight.heal(level, pos);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && entity instanceof LivingEntity living && !Plague.wearsMask(living)
                && level.random.nextDouble() < PestConfig.BLIGHT_STEP_INFECT_CHANCE.get()) {
            Plague.infect(living);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0 && level.getBlockState(pos.above()).isAir()) {
            level.addParticle(ParticleTypes.MYCELIUM, pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(), 0, 0, 0);
        }
    }
}
