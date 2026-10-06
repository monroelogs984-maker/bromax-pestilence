package com.bromax.pestilence.disease;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Which disease a sick entity has, and whether a physician has named it yet. Stored as an entity attachment. */
public record Ailment(Disease disease, boolean diagnosed) {
    public static final Ailment NONE = new Ailment(Disease.BUBONIC, false);
    public static final Codec<Ailment> CODEC = RecordCodecBuilder.create(i -> i.group(
            Disease.CODEC.fieldOf("disease").forGetter(Ailment::disease),
            Codec.BOOL.fieldOf("diagnosed").forGetter(Ailment::diagnosed)
    ).apply(i, Ailment::new));

    public Ailment diagnose() {
        return new Ailment(disease, true);
    }
}
