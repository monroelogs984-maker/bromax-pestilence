package com.bromax.pestilence.item;

import com.bromax.pestilence.world.Blight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Scattered on blighted ground or withered leaves it cleans a 3x3x3 patch at once. Also fills in
 * Plague Pits (see NestBlock) and dissolves corpses (see Corpse).
 */
public class QuicklimeItem extends Item {
    public QuicklimeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!Blight.isBlighted(context.getLevel().getBlockState(context.getClickedPos()))) return InteractionResult.PASS;
        if (context.getLevel() instanceof ServerLevel level) {
            BlockPos center = context.getClickedPos();
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) Blight.heal(level, p);
            level.sendParticles(ParticleTypes.WHITE_ASH, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 40, 1, 0.4, 1, 0.02);
            level.playSound(null, center, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 1f, 0.8f);
            if (context.getPlayer() != null) context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
