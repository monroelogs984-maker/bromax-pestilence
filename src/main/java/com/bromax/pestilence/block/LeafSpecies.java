package com.bromax.pestilence.block;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/** Which vanilla leaves a withered leaves block came from, so the Blight can be undone exactly. */
public enum LeafSpecies implements StringRepresentable {
    OAK(Blocks.OAK_LEAVES), SPRUCE(Blocks.SPRUCE_LEAVES), BIRCH(Blocks.BIRCH_LEAVES), JUNGLE(Blocks.JUNGLE_LEAVES),
    ACACIA(Blocks.ACACIA_LEAVES), DARK_OAK(Blocks.DARK_OAK_LEAVES), MANGROVE(Blocks.MANGROVE_LEAVES),
    CHERRY(Blocks.CHERRY_LEAVES), AZALEA(Blocks.AZALEA_LEAVES), FLOWERING_AZALEA(Blocks.FLOWERING_AZALEA_LEAVES);

    public final Block leaves;

    LeafSpecies(Block leaves) {
        this.leaves = leaves;
    }

    @Nullable
    public static LeafSpecies of(Block block) {
        for (LeafSpecies s : values()) if (s.leaves == block) return s;
        return null;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
}
