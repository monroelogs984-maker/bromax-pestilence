package com.bromax.pestilence.block;

import com.bromax.pestilence.ModRegistry;
import com.bromax.pestilence.PestConfig;
import com.bromax.pestilence.entity.MiasmaCloud;
import com.bromax.pestilence.world.Blight;
import com.bromax.pestilence.world.Tracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

public class BrazierBlockEntity extends BlockEntity {
    private static final int MAX_HERBS = 4;
    private int fuel;

    public BrazierBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.BRAZIER_BE.get(), pos, state);
    }

    public boolean canTakeFuel() {
        return fuel <= PestConfig.BRAZIER_FUEL_TICKS.get() * (MAX_HERBS - 1);
    }

    public void addFuel() {
        fuel += PestConfig.BRAZIER_FUEL_TICKS.get();
        setLit(true);
        setChanged();
    }

    private void setLit(boolean lit) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.getValue(HerbBrazierBlock.LIT) != lit) level.setBlock(worldPosition, state.setValue(HerbBrazierBlock.LIT, lit), Block.UPDATE_ALL);
        Tracker.setBrazier(level, worldPosition, lit);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BrazierBlockEntity brazier) {
        if (brazier.fuel <= 0 || !(level instanceof ServerLevel server)) return;
        if (--brazier.fuel <= 0) {
            brazier.setLit(false);
            brazier.setChanged();
            return;
        }
        if (brazier.fuel % 40 != 0) return;
        brazier.setChanged();
        int r = PestConfig.BRAZIER_RADIUS.get();
        for (MiasmaCloud cloud : server.getEntitiesOfClass(MiasmaCloud.class, new AABB(pos).inflate(r))) cloud.discard();
        // Heal a few spots on the surface (and the treetops) inside the circle.
        for (int i = 0; i < 6; i++) {
            float angle = server.random.nextFloat() * Mth.TWO_PI;
            double dist = Math.sqrt(server.random.nextDouble()) * r;
            int x = pos.getX() + Mth.floor(Mth.cos(angle) * dist), z = pos.getZ() + Mth.floor(Mth.sin(angle) * dist);
            BlockPos top = new BlockPos(x, server.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
            for (int dy = 0; dy < 8 && !Blight.heal(server, top.below(dy)); dy++) {
                if (!server.getBlockState(top.below(dy)).isAir() && !Blight.isBlighted(server.getBlockState(top.below(dy)))
                        && server.getBlockState(top.below(dy)).isSolidRender(server, top.below(dy))) break;
            }
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && fuel > 0) Tracker.setBrazier(level, worldPosition, true);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) Tracker.setBrazier(level, worldPosition, false);
    }

    @Override
    public void onChunkUnloaded() {
        if (level != null && !level.isClientSide) Tracker.setBrazier(level, worldPosition, false);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fuel = tag.getInt("fuel");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("fuel", fuel);
    }
}
