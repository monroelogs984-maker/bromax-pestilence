package com.bromax.pestilence.block;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.entity.PlagueRat;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A plague nest: Rat Warren, Plague Pit or Miasma Well. Grows from stage I to IV (harder to mine
 * each stage); its behaviour lives in {@link NestBlockEntity}. Breaking one weakens the Pestilence.
 * A Plague Pit is nearly unmineable but quicklime fills it in one stage at a time.
 */
public class NestBlock extends BaseEntityBlock {
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 1, 4);
    private static final VoxelShape[] SHAPES = {
            Block.box(0, 0, 0, 16, 8, 16), Block.box(0, 0, 0, 16, 10, 16),
            Block.box(0, 0, 0, 16, 13, 16), Block.box(0, 0, 0, 16, 16, 16)};
    private static final int WELL_COLOR = 0x6b7330;

    private final NestKind kind;
    private final MapCodec<NestBlock> codec;

    public NestBlock(NestKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(p -> new NestBlock(kind, p));
        registerDefaultState(stateDefinition.any().setValue(STAGE, 1));
    }

    public NestKind kind() {
        return kind;
    }

    /** Push a nest up one stage (a Bone Collector feeding it). False if it is already stage IV. */
    public static boolean grow(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof NestBlock) || state.getValue(STAGE) >= 4) return false;
        level.setBlock(pos, state.setValue(STAGE, state.getValue(STAGE) + 1), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.BLOCKS, 1f, 0.6f);
        return true;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind == NestKind.WARREN ? SHAPES[state.getValue(STAGE) - 1] : SHAPES[3];
    }

    /** Each stage makes the nest proportionally harder to break. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return super.getDestroyProgress(state, player, level, pos) / state.getValue(STAGE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NestBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModRegistry.NEST_BE.get(), NestBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (kind != NestKind.PIT || !stack.is(ModRegistry.QUICKLIME.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level instanceof ServerLevel server) {
            int stage = state.getValue(STAGE);
            stack.consume(1, player);
            server.sendParticles(ParticleTypes.WHITE_ASH, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 30, 0.5, 0.3, 0.5, 0.02);
            server.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1f, 0.7f);
            if (stage > 1) {
                server.setBlock(pos, state.setValue(STAGE, stage - 1), Block.UPDATE_ALL);
            } else {
                server.removeBlock(pos, false);
                destroyed(server, pos, 1, player);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Runs for any player break (whatever the tool); creative breaks skip the drops and the rats. */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockState result = super.playerWillDestroy(level, pos, state, player);
        if (level instanceof ServerLevel server && !player.isCreative()) broken(server, pos, state, player);
        return result;
    }

    private void broken(ServerLevel server, BlockPos pos, BlockState state, Player player) {
        Level level = server;
        int stage = state.getValue(STAGE);
        switch (kind) {
            case WARREN -> {
                popResource(level, pos, new ItemStack(ModRegistry.RAT_TAIL.get(), stage));
                // Disturbing a warren lets loose whatever was still inside.
                for (int i = 0; i < stage * 2; i++) {
                    PlagueRat rat = ModRegistry.PLAGUE_RAT.get().create(server);
                    if (rat == null) continue;
                    rat.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, server.random.nextFloat() * 360f, 0f);
                    rat.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
                    rat.setTarget(player);
                    server.addFreshEntity(rat);
                }
            }
            case PIT -> popResource(level, pos, new ItemStack(Items.BONE, 2 * stage));
            case WELL -> popResource(level, pos, new ItemStack(Items.GLOWSTONE_DUST, stage));
        }
        destroyed(server, pos, stage, player);
    }

    private void destroyed(ServerLevel level, BlockPos pos, int stage, Player player) {
        com.bromax.pestilence.disease.Consecration.deed(player, com.bromax.pestilence.disease.Consecration.NEST);
        Plague.addPoints(player, PestConfig.POINTS_NEST_DESTROYED.get() * stage);
        player.displayClientMessage(Component.translatable("message." + Pestilence.MODID + ".nest_destroyed." + kind.id)
                .withStyle(ChatFormatting.DARK_GREEN), true);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double x = pos.getX() + 0.5, y = pos.getY() + 0.8, z = pos.getZ() + 0.5;
        int stage = state.getValue(STAGE);
        switch (kind) {
            case WARREN -> {
                if (random.nextInt(60) < stage) level.playLocalSound(x, y, z, SoundEvents.SILVERFISH_AMBIENT, SoundSource.HOSTILE, 0.5f, 1.5f, false);
                if (random.nextInt(4) == 0) level.addParticle(ModRegistry.FLY.get(), x + random.nextGaussian() * 0.4, y + 0.3, z + random.nextGaussian() * 0.4, 0, 0, 0);
            }
            case PIT -> {
                if (random.nextInt(3) == 0) level.addParticle(ModRegistry.FLY.get(), x + random.nextGaussian() * 0.5, y + 0.6, z + random.nextGaussian() * 0.5, 0, 0, 0);
                if (random.nextInt(5) == 0) level.addParticle(ParticleTypes.SMOKE, x + random.nextGaussian() * 0.3, y + 0.3, z + random.nextGaussian() * 0.3, 0, 0.02, 0);
            }
            case WELL -> {
                for (int i = 0; i < stage; i++) {
                    level.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, WELL_COLOR),
                            x + random.nextGaussian() * 0.25, y + 0.4, z + random.nextGaussian() * 0.25, 0, 0, 0);
                }
                if (random.nextInt(40) == 0) level.playLocalSound(x, y, z, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.6f, 0.6f, false);
            }
        }
    }
}
