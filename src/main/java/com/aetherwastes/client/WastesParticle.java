package com.aetherwastes.client;

import com.aetherwastes.client.fx.ParticleBudget;
import com.aetherwastes.client.fx.PhysicsParticle;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * Светящиеся анимированные частицы Пустошей. Поведение задаётся видом:
 * <ul>
 *   <li>SPARK — искры: летят по дуге под гравитацией, отскакивают от блоков, гаснут после удара;</li>
 *   <li>WISP — призрачные огни: всплывают и вьются, физики нет;</li>
 *   <li>RUNE — руны: раскрываются и медленно вращаются на месте;</li>
 *   <li>EMBER — угли: поднимаются тёплым потоком, упираются в потолок, дрожат.</li>
 * </ul>
 * Все виды проходят через {@link ParticleBudget} и светятся сами (полная яркость).
 */
public class WastesParticle extends PhysicsParticle {
    public enum Kind {SPARK, WISP, RUNE, EMBER}

    private final SpriteSet sprites;
    private final Kind kind;
    private final float baseSize;
    private final float spin;

    protected WastesParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites, Kind kind) {
        super(level, x, y, z,
                xd + (level.random.nextFloat() - 0.5f) * 0.02f,
                yd + (level.random.nextFloat() - 0.5f) * 0.02f,
                zd + (level.random.nextFloat() - 0.5f) * 0.02f);
        this.sprites = sprites;
        this.kind = kind;
        switch (kind) {
            case SPARK -> {
                lifetime = 16 + random.nextInt(14);
                quadSize = 0.07f + random.nextFloat() * 0.05f;
                friction = 0.96f;
                gravity = 0.55f;
                restitution = 0.5f;
                yd += 0.04f; // искра сначала подскакивает
            }
            case WISP -> {
                lifetime = 26 + random.nextInt(18);
                quadSize = 0.14f + random.nextFloat() * 0.12f;
                friction = 0.94f;
                gravity = -0.012f;
                collides = false;
            }
            case RUNE -> {
                lifetime = 30 + random.nextInt(16);
                quadSize = 0.16f + random.nextFloat() * 0.06f;
                friction = 0.9f;
                gravity = -0.006f;
                collides = false;
            }
            case EMBER -> {
                lifetime = 24 + random.nextInt(24);
                quadSize = 0.05f + random.nextFloat() * 0.05f;
                friction = 0.96f;
                gravity = -0.02f;
                restitution = 0.2f;
            }
        }
        setSize(0.05f, 0.05f);
        this.baseSize = quadSize;
        this.spin = (random.nextFloat() - 0.5f) * 0.2f;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = roll;
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        oRoll = roll;
        if (!stepPhysics()) return;
        setSpriteFromAge(sprites);
        float t = (float) age / lifetime;
        if (kind == Kind.RUNE || kind == Kind.SPARK) roll += spin;
        if (kind == Kind.WISP) {
            xd += Mth.sin(age * 0.3f + (float) x) * 0.003f;
            zd += Mth.cos(age * 0.3f + (float) z) * 0.003f;
        }
        if (kind == Kind.EMBER) xd += (random.nextFloat() - 0.5f) * 0.004f;
        if (kind == Kind.SPARK && impacts > 0 && age < lifetime - 6) age = Math.max(age, lifetime - 6); // гаснет после удара
        quadSize = baseSize * (kind == Kind.RUNE ? Math.min(1f, t * 5f) : 1f - t * 0.6f);
        alpha = t > 0.7f ? 1f - (t - 0.7f) / 0.3f : 1f;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites, Kind kind) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            if (!ParticleBudget.allow(level, x, y, z)) return null;
            return new WastesParticle(level, x, y, z, xd, yd, zd, sprites, kind);
        }
    }
}
