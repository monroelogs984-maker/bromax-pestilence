package com.bromax.pestilence.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** A tiny dark fly that buzzes in a jittery loop around where it spawned. */
public class FlyParticle extends TextureSheetParticle {
    private final double homeX, homeY, homeZ;

    protected FlyParticle(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        homeX = x;
        homeY = y;
        homeZ = z;
        lifetime = 20 + random.nextInt(30);
        quadSize = 0.035f + random.nextFloat() * 0.015f;
        gravity = 0f;
        hasPhysics = false;
    }

    @Override
    public void tick() {
        super.tick();
        xd += (random.nextDouble() - 0.5) * 0.04 + (homeX - x) * 0.02;
        yd += (random.nextDouble() - 0.5) * 0.03 + (homeY - y) * 0.02;
        zd += (random.nextDouble() - 0.5) * 0.04 + (homeZ - z) * 0.02;
        xd *= 0.85;
        yd *= 0.85;
        zd *= 0.85;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            FlyParticle fly = new FlyParticle(level, x, y, z);
            fly.pickSprite(sprites);
            return fly;
        }
    }
}
