package com.bromax.pestilence.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

/** A medicinal herb: grows wild on grass in its biome, and can be planted (with the herb itself) on farmland or dirt. */
public class HerbBlock extends CropBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = {Block.box(0, 0, 0, 16, 4, 16), Block.box(0, 0, 0, 16, 7, 16),
            Block.box(0, 0, 0, 16, 10, 16), Block.box(0, 0, 0, 16, 13, 16)};

    private final Supplier<? extends ItemLike> herb;
    private final MapCodec<HerbBlock> codec;

    public HerbBlock(Supplier<? extends ItemLike> herb, Properties properties) {
        super(properties);
        this.herb = herb;
        this.codec = simpleCodec(p -> new HerbBlock(herb, p));
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return codec;
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return 3;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return herb.get();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getAge(state)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.FARMLAND) || state.is(BlockTags.DIRT);
    }
}
