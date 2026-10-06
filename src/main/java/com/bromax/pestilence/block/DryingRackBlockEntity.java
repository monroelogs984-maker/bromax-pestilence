package com.bromax.pestilence.block;

import com.bromax.pestilence.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public class DryingRackBlockEntity extends ItemHolderBlockEntity {
    public static final int DRY_TICKS = 1200;
    private final int[] progress = new int[4];

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DRYING_RACK_BE.get(), pos, state, 4);
    }

    private static Map<Item, Item> dried() {
        return ModRegistry.driedForms();
    }

    public static boolean canDry(ItemStack stack) {
        return dried().containsKey(stack.getItem());
    }

    public boolean hang(ItemStack herb) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, herb);
                progress[i] = 0;
                changed();
                return true;
            }
        }
        return false;
    }

    /** Dried herbs come down first, then anything else. */
    public ItemStack takeOne() {
        for (int pass = 0; pass < 2; pass++) {
            for (int i = items.size() - 1; i >= 0; i--) {
                ItemStack s = items.get(i);
                if (!s.isEmpty() && (pass == 1 || !canDry(s))) {
                    items.set(i, ItemStack.EMPTY);
                    changed();
                    return s;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DryingRackBlockEntity rack) {
        boolean any = false;
        for (int i = 0; i < rack.items.size(); i++) {
            Item result = dried().get(rack.items.get(i).getItem());
            if (result == null) continue;
            if (++rack.progress[i] >= DRY_TICKS) {
                rack.items.set(i, new ItemStack(result));
                rack.progress[i] = 0;
                any = true;
            }
        }
        if (any) rack.changed();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] p = tag.getIntArray("progress");
        System.arraycopy(p, 0, progress, 0, Math.min(p.length, progress.length));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("progress", progress);
    }
}
