package com.bromax.pestilence.block;

import com.bromax.pestilence.phase.Phase;

/** The three kinds of plague nest, the phase each starts appearing at, and how hard each is to mine per stage. */
public enum NestKind {
    WARREN("rat_warren", Phase.RAT_SEASON, 1.5f, 3),
    PIT("plague_pit", Phase.HERALDS, 25f, 2),
    WELL("miasma_well", Phase.BLACK_DEATH, 3f, 1);

    public final String id;
    public final int phase;
    public final float hardness;
    /** Relative odds when a new nest is seeded. */
    public final int weight;

    NestKind(String id, int phase, float hardness, int weight) {
        this.id = id;
        this.phase = phase;
        this.hardness = hardness;
        this.weight = weight;
    }
}
