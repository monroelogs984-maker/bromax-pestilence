package com.bromax.pestilence.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A board daubed with a red cross: this house is shut up with the plague. Sick villagers within
 * {@link #RADIUS} blocks of one keep to the house and cannot pass the sickness on.
 */
public class PlagueCrossBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PlagueCrossBlock> CODEC = simpleCodec(PlagueCrossBlock::new);
    public static final int RADIUS = 5;
    private static final VoxelShape NORTH = Block.box(1, 1, 15, 15, 15, 16);
    private static final VoxelShape SOUTH = Block.box(1, 1, 0, 15, 15, 1);
    private static final VoxelShape EAST = Block.box(0, 1, 1, 1, 15, 15);
    private static final VoxelShape WEST = Block.box(15, 1, 1, 16, 15, 15);

    public PlagueCrossBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Hangs on the face it was placed against, painted side out (facing the player). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace().getAxis().isHorizontal() ? context.getClickedFace() : context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, face);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    public static boolean quarantined(Level level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-RADIUS, -2, -RADIUS), pos.offset(RADIUS, 3, RADIUS))) {
            if (level.getBlockState(p).getBlock() instanceof PlagueCrossBlock) return true;
        }
        return false;
    }
}
