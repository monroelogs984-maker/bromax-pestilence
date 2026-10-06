package com.bromax.pestilence.block;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.world.Blight;
import com.bromax.pestilence.world.Tracker;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/** Blighted leaves. Behave like leaves (they decay), remember their species, and recover once no nest is near. */
public class WitheredLeavesBlock extends LeavesBlock {
    public static final MapCodec<WitheredLeavesBlock> CODEC = simpleCodec(WitheredLeavesBlock::new);
    public static final EnumProperty<LeafSpecies> SPECIES = EnumProperty.create("species", LeafSpecies.class);

    public WitheredLeavesBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(SPECIES, LeafSpecies.OAK));
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SPECIES);
    }

    /** The withered version of natural vanilla leaves, or null for anything else (player-placed leaves are left alone). */
    @Nullable
    public static BlockState witherFrom(BlockState leaves) {
        LeafSpecies species = LeafSpecies.of(leaves.getBlock());
        if (species == null || leaves.getValue(PERSISTENT)) return null;
        return ModRegistry.WITHERED_LEAVES.get().defaultBlockState()
                .setValue(SPECIES, species)
                .setValue(DISTANCE, leaves.getValue(DISTANCE))
                .setValue(WATERLOGGED, leaves.getValue(WATERLOGGED));
    }

    public static BlockState restore(BlockState withered) {
        return withered.getValue(SPECIES).leaves.defaultBlockState()
                .setValue(DISTANCE, withered.getValue(DISTANCE))
                .setValue(PERSISTENT, withered.getValue(PERSISTENT))
                .setValue(WATERLOGGED, withered.getValue(WATERLOGGED));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (level.getBlockState(pos).is(this) && random.nextInt(3) == 0
                && !Tracker.nestWithin(level, pos, PestConfig.BLIGHT_HEAL_RANGE.get())) {
            Blight.heal(level, pos);
        }
    }
}
