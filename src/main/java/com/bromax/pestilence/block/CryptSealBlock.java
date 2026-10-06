package com.bromax.pestilence.block;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.Pestilence;
import com.bromax.pestilence.entity.boss.Gravewarden;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The seal on the Lazaretto's crypt. Break it (use it) and the Gravewarden rises. You decide when. */
public class CryptSealBlock extends Block {
    public static final MapCodec<CryptSealBlock> CODEC = simpleCodec(CryptSealBlock::new);
    public static final BooleanProperty BROKEN = BooleanProperty.create("broken");

    public CryptSealBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BROKEN, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BROKEN);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(BROKEN)) return InteractionResult.PASS;
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, state.setValue(BROKEN, true), Block.UPDATE_ALL);
            Gravewarden warden = ModRegistry.GRAVEWARDEN.get().create(server);
            if (warden != null) {
                warden.moveTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, player.getYRot() + 180f, 0f);
                warden.finalizeSpawn(server, server.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null);
                warden.setTarget(player);
                server.addFreshEntity(warden);
            }
            server.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.5f, 0.6f);
            server.sendParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 40, 1, 0.5, 1, 0.05);
            Component msg = Component.translatable("message." + Pestilence.MODID + ".seal_broken").withStyle(ChatFormatting.DARK_RED);
            server.players().stream().filter(p -> p.distanceToSqr(player) < 64 * 64).forEach(p -> p.sendSystemMessage(msg));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
