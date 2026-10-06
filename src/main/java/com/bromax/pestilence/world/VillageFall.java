package com.bromax.pestilence.world;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.Plague;
import com.bromax.pestilence.block.NestKind;
import com.bromax.pestilence.block.PlagueCrossBlock;
import com.bromax.pestilence.disease.Disease;
import com.bromax.pestilence.entity.Corpse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;

/**
 * A fallen village: plague crosses go up beside the doors, a Plague Pit opens near the bell, and
 * (when found already fallen) the villagers are dead or dying: their corpses will rise.
 */
public final class VillageFall {
    private VillageFall() {}

    public static void fall(ServerLevel level, BlockPos centre, boolean discovered) {
        if (discovered) {
            for (Villager v : level.getEntitiesOfClass(Villager.class, new AABB(centre).inflate(VillageOutbreaks.RADIUS))) {
                if (level.random.nextBoolean()) {
                    Corpse.place(level, EntityType.VILLAGER, level.random.nextInt(PestConfig.CORPSE_RISE_TICKS.get() / 2),
                            v.getX(), v.getY(), v.getZ(), v.getYRot());
                    v.discard();
                } else {
                    Plague.force(v, Disease.BUBONIC, Plague.MAX_STAGE);
                }
            }
        }
        markDoors(level, centre);
        NestSeeding.placeNear(level, centre, NestKind.PIT, 12);
        VillageOutbreaks.announce(level, centre, discovered ? "village_found_fallen" : "village_fallen", ChatFormatting.DARK_RED);
    }

    /** A plague cross on the outside of every door near the village centre (at most 24). */
    private static void markDoors(ServerLevel level, BlockPos centre) {
        int placed = 0;
        int r = VillageOutbreaks.RADIUS - 8;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-r, -8, -r), centre.offset(r, 8, r))) {
            if (placed >= 24) return;
            BlockState state = level.getBlockState(p);
            if (!(state.getBlock() instanceof DoorBlock) || state.getValue(DoorBlock.HALF) != DoubleBlockHalf.UPPER) continue;
            Direction facing = state.getValue(DoorBlock.FACING);
            for (Direction side : new Direction[]{facing, facing.getOpposite()}) {
                BlockPos at = p.relative(side);
                if (level.getBlockState(at).isAir() && level.canSeeSky(at)) {
                    level.setBlock(at, ModRegistry.PLAGUE_CROSS.get().defaultBlockState().setValue(PlagueCrossBlock.FACING, side), Block.UPDATE_ALL);
                    placed++;
                    break;
                }
            }
        }
    }
}
