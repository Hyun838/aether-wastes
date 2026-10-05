package com.aetherwastes.client.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Объёмная частица-осколок: вытянутый октаэдр из 8 граней, который кувыркается в полёте,
 * отскакивает от блоков и освещается по нормалям граней (направленный свет + подсветка контура),
 * поэтому выглядит как настоящий кристалл, а не плоская картинка.
 *
 * <p>LOD: дальше {@link #MESH_LOD} блоков рисуется обычный плоский спрайт (1 квад вместо 8),
 * столкновения выключаются ещё раньше (см. {@link PhysicsParticle}). Все временные векторы
 * переиспользуются — в кадре нет аллокаций.
 */
public class ShardParticle extends PhysicsParticle {
    private static final double MESH_LOD = 20.0;

    /** Вершины октаэдра, вытянутого по Y. */
    private static final Vector3f[] VERTS = {
            new Vector3f(0, 1.7f, 0), new Vector3f(0, -1.2f, 0),
            new Vector3f(0.75f, 0, 0), new Vector3f(-0.75f, 0, 0), new Vector3f(0, 0, 0.75f), new Vector3f(0, 0, -0.75f)};
    private static final int[][] FACES = new int[8][];
    private static final Vector3f[] NORMALS = new Vector3f[8];
    /** Направление «солнца» в мировых координатах для затенения граней. */
    private static final Vector3f LIGHT = new Vector3f(0.35f, 1f, 0.55f).normalize();

    static {
        int[][] raw = {{0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2}, {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}};
        for (int i = 0; i < raw.length; i++) {
            int[] f = raw[i];
            Vector3f a = VERTS[f[0]], b = VERTS[f[1]], c = VERTS[f[2]];
            Vector3f n = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
            Vector3f centroid = new Vector3f(a).add(b).add(c);
            if (n.dot(centroid) < 0) { // выворачиваем грань наружу
                f = new int[]{f[0], f[2], f[1]};
                n.negate();
            }
            FACES[i] = f;
            NORMALS[i] = n;
        }
    }

    private final SpriteSet sprites;
    private final Quaternionf rot = new Quaternionf();
    private final Quaternionf oRot = new Quaternionf();
    private final Vector3f axis;
    private float spin;
    private final boolean glow;

    // Переиспользуемые буферы для рендера
    private final Quaternionf q = new Quaternionf();
    private final Vector3f tmp = new Vector3f();
    private final Vector3f[] world = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};

    protected ShardParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                            SpriteSet sprites, float r, float g, float b, boolean glow) {
        super(level, x, y, z, xd, yd, zd);
        this.sprites = sprites;
        this.glow = glow;
        this.lifetime = 40 + random.nextInt(30);
        this.quadSize = 0.06f + random.nextFloat() * 0.07f;
        this.gravity = 0.9f;
        this.friction = 0.98f;
        this.restitution = 0.45f;
        this.groundFriction = 0.6f;
        float tint = 0.85f + random.nextFloat() * 0.15f;
        this.rCol = r * tint;
        this.gCol = g * tint;
        this.bCol = b * tint;
        setSize(quadSize * 1.5f, quadSize * 1.5f);
        this.axis = new Vector3f(random.nextFloat() - 0.5f, random.nextFloat() - 0.5f, random.nextFloat() - 0.5f).normalize();
        this.spin = 0.15f + random.nextFloat() * 0.35f;
        this.rot.rotateAxis(random.nextFloat() * Mth.TWO_PI, axis);
        this.oRot.set(rot);
        pickSprite(sprites);
    }

    @Override
    public void tick() {
        oRot.set(rot);
        if (!stepPhysics()) return;
        if (impacts > 0) spin *= 0.85f;          // удар гасит вращение
        if (!resting) rot.rotateAxis(spin, axis);
        float t = (float) age / lifetime;
        alpha = t > 0.75f ? 1f - (t - 0.75f) / 0.25f : 1f;
    }

    @Override
    public void render(VertexConsumer vc, Camera camera, float pt) {
        Vec3 cam = camera.getPosition();
        float px = (float) (Mth.lerp(pt, xo, x) - cam.x);
        float py = (float) (Mth.lerp(pt, yo, y) - cam.y);
        float pz = (float) (Mth.lerp(pt, zo, z) - cam.z);
        if (px * px + py * py + pz * pz > MESH_LOD * MESH_LOD) {
            super.render(vc, camera, pt); // дальний LOD: плоский спрайт
            return;
        }
        oRot.slerp(rot, pt, q);
        float size = quadSize;
        for (int i = 0; i < VERTS.length; i++) {
            q.transform(VERTS[i], world[i]).mul(size).add(px, py, pz);
        }
        float u0 = getU0(), u1 = getU1(), v0 = getV0(), v1 = getV1();
        float um = (u0 + u1) * 0.5f;
        int light = getLightColor(pt);
        for (int i = 0; i < FACES.length; i++) {
            q.transform(NORMALS[i], tmp);
            float diffuse = Math.max(0f, tmp.dot(LIGHT));
            // Подсветка контура: грани, повёрнутые ребром к камере, светлее (эффект преломления).
            float facing = Math.abs(tmp.dot(-px, -py, -pz) / Math.max(1e-4f, Mth.sqrt(px * px + py * py + pz * pz)));
            float rim = (1f - facing) * 0.35f;
            float k = Math.min(1.25f, 0.45f + 0.6f * diffuse + rim);
            float r = Math.min(1f, rCol * k), g = Math.min(1f, gCol * k), b = Math.min(1f, bCol * k);
            int[] f = FACES[i];
            Vector3f a = world[f[0]], bb = world[f[1]], c = world[f[2]];
            // Треугольник как вырожденный квад (формат частиц — QUADS)
            vc.addVertex(a.x, a.y, a.z).setUv(um, v0).setColor(r, g, b, alpha).setLight(light);
            vc.addVertex(bb.x, bb.y, bb.z).setUv(u0, v1).setColor(r, g, b, alpha).setLight(light);
            vc.addVertex(c.x, c.y, c.z).setUv(u1, v1).setColor(r, g, b, alpha).setLight(light);
            vc.addVertex(c.x, c.y, c.z).setUv(u1, v1).setColor(r, g, b, alpha).setLight(light);
        }
    }

    @Override
    public int getLightColor(float pt) {
        if (glow) return LightTexture.FULL_BRIGHT;
        int base = super.getLightColor(pt);
        return LightTexture.pack(Math.max(LightTexture.block(base), 11), LightTexture.sky(base));
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Фабрика: цвет задаётся типом частицы (эфирный, пепельный, призрачный осколок). */
    public record Provider(SpriteSet sprites, float r, float g, float b, boolean glow) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            if (!ParticleBudget.allow(level, x, y, z)) return null;
            return new ShardParticle(level, x, y, z, xd, yd, zd, sprites, r, g, b, glow);
        }
    }
}
