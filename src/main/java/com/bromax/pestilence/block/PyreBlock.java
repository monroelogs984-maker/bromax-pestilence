package com.bromax.pestilence.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A log pyre. Light it with flint and steel or a fire charge; while lit it burns any plague corpse
 * within a few blocks and scorches whoever stands in it. A shovel puts it out.
 */
public class PyreBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 10, 16);

    public PyreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        boolean lit = state.getValue(LIT);
        if (!lit && (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE) || stack.is(com.bromax.pestilence.ModRegistry.PYRE_TORCH.get()))) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            level.playSound(player, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1f, 1f);
            if (!level.isClientSide) {
                if (stack.isDamageableItem()) stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
                else stack.consume(1, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (lit && stack.getItem() instanceof ShovelItem) {
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
            level.playSound(player, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1f, 1f);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (state.getValue(LIT) && entity instanceof LivingEntity && !entity.fireImmune()) {
            entity.hurt(level.damageSources().inFire(), 1f);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;
        if (random.nextInt(8) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.CAMPFIRE_CRACKLE,
                    SoundSource.BLOCKS, 1f, 0.8f, false);
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.7,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.03, 0);
        }
        level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.4, pos.getY() + 1.0,
                pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.4, 0, 0.07, 0);
    }
}
