package com.aetherwastes.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Ударная волна: светящееся кольцо, лежащее на земле и расходящееся до заданного радиуса.
 * Рисуется горизонтально (не повёрнуто к камере), с обеих сторон.
 *
 * <p>Параметры передаются через скорость при спавне с count = 0:
 * {@code sendParticles(SHOCKWAVE, x, y, z, 0, radius, palette, 0, 1)}, где palette:
 * 0 — эфир (голубой), 1 — пепел (оранжевый), 2 — призрак (фиолетовый).
 */
public class ShockwaveParticle extends TextureSheetParticle {
    private static final float[][] PALETTE = {{0.55f, 0.95f, 1f}, {1f, 0.55f, 0.2f}, {0.75f, 0.55f, 1f}};

    private final SpriteSet sprites;
    private final float radius;

    protected ShockwaveParticle(ClientLevel level, double x, double y, double z, double radius, int palette, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.radius = (float) Mth.clamp(radius, 0.5, 24.0);
        this.lifetime = 12 + (int) (this.radius * 1.2f);
        this.gravity = 0;
        this.hasPhysics = false;
        this.xd = this.yd = this.zd = 0;
        float[] c = PALETTE[Mth.clamp(palette, 0, PALETTE.length - 1)];
        this.rCol = c[0];
        this.gCol = c[1];
        this.bCol = c[2];
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        setSpriteFromAge(sprites);
    }

    @Override
    public float getQuadSize(float pt) {
        float t = Math.min(1f, (age + pt) / lifetime);
        float ease = 1f - (1f - t) * (1f - t) * (1f - t); // быстрый старт, плавное замедление
        return radius * ease;
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float pt) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(pt, xo, x) - cam.x);
        float py = (float) (Mth.lerp(pt, yo, y) - cam.y) + 0.05f;
        float pz = (float) (Mth.lerp(pt, zo, z) - cam.z);
        float s = getQuadSize(pt);
        float t = Math.min(1f, (age + pt) / lifetime);
        float a = (1f - t) * 0.9f;
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        int light = LightTexture.FULL_BRIGHT;
        // верхняя сторона
        vc.addVertex(px - s, py, pz - s).setUv(u0, v0).setColor(rCol, gCol, bCol, a).setLight(light);
        vc.addVertex(px - s, py, pz + s).setUv(u0, v1).setColor(rCol, gCol, bCol, a).setLight(light);
        vc.addVertex(px + s, py, pz + s).setUv(u1, v1).setColor(rCol, gCol, bCol, a).setLight(light);
        vc.addVertex(px + s, py, pz - s).setUv(u1, v0).setColor(rCol, gCol, bCol, a).setLight(light);
        // нижняя сторона (видна из-под уступа)
        vc.addVertex(px + s, py, pz - s).setUv(u1, v0).setColor(rCol, gCol, bCol, a).setLight(light);
        vc.addVertex(px + s, py, pz + s).setUv(u1, v1).setColor(rCol, gCol, bCol, a).setLight(light);
        vc.addVertex(px - s, py, pz + s).setUv(u0, v1).setColor(rCol, gCol, bCol, a).setLight(light);
        vc.addVertex(px - s, py, pz - s).setUv(u0, v0).setColor(rCol, gCol, bCol, a).setLight(light);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(float partialTicks) {
        return new net.minecraft.world.phys.AABB(x - radius, y - 0.5, z - radius, x + radius, y + 0.5, z + radius);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            if (!ParticleBudget.allow(level, x, y, z)) return null;
            return new ShockwaveParticle(level, x, y, z, xd, (int) Math.round(yd), sprites);
        }
    }
}
