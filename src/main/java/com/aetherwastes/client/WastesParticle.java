package com.aetherwastes.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/** Светящиеся анимированные частицы Пустошей. Поведение задаётся видом. */
public class WastesParticle extends TextureSheetParticle {
    public enum Kind {SPARK, WISP, RUNE, EMBER}

    private final SpriteSet sprites;
    private final Kind kind;
    private final float baseSize;
    private final float spin;

    protected WastesParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, Kind kind) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.kind = kind;
        this.hasPhysics = false;
        this.xd = xd + (random.nextFloat() - 0.5f) * 0.02f;
        this.yd = yd + (random.nextFloat() - 0.5f) * 0.02f;
        this.zd = zd + (random.nextFloat() - 0.5f) * 0.02f;
        switch (kind) {
            case SPARK -> {
                lifetime = 12 + random.nextInt(10);
                quadSize = 0.08f + random.nextFloat() * 0.06f;
                friction = 0.9f;
                gravity = 0f;
            }
            case WISP -> {
                lifetime = 26 + random.nextInt(18);
                quadSize = 0.14f + random.nextFloat() * 0.12f;
                friction = 0.94f;
                gravity = -0.012f;
            }
            case RUNE -> {
                lifetime = 30 + random.nextInt(16);
                quadSize = 0.16f + random.nextFloat() * 0.06f;
                friction = 0.9f;
                gravity = -0.006f;
            }
            case EMBER -> {
                lifetime = 24 + random.nextInt(24);
                quadSize = 0.05f + random.nextFloat() * 0.05f;
                friction = 0.96f;
                gravity = -0.02f;
            }
        }
        this.baseSize = quadSize;
        this.spin = (random.nextFloat() - 0.5f) * 0.2f;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (removed) return;
        setSpriteFromAge(sprites);
        float t = (float) age / lifetime;
        oRoll = roll;
        if (kind == Kind.RUNE || kind == Kind.SPARK) roll += spin;
        if (kind == Kind.WISP) {
            xd += Mth.sin(age * 0.3f + (float) x) * 0.003f;
            zd += Mth.cos(age * 0.3f + (float) z) * 0.003f;
        }
        if (kind == Kind.EMBER) xd += (random.nextFloat() - 0.5f) * 0.004f;
        quadSize = baseSize * (kind == Kind.RUNE ? Math.min(1f, t * 5f) : 1f - t * 0.6f);
        alpha = t > 0.7f ? 1f - (t - 0.7f) / 0.3f : 1f;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            return new WastesParticle(level, x, y, z, xd, yd, zd, sprites, kind);
        }
    }
}
