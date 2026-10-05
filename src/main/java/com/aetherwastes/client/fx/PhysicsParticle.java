package com.aetherwastes.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Частица с настоящей физикой: гравитация, сопротивление воздуха, столкновения с блоками
 * и отскок с потерей энергии. В отличие от ванильной частицы, не «залипает» навсегда
 * после первого касания стены.
 *
 * <p>Оптимизация: столкновения считаются только рядом с камерой ({@link #PHYSICS_LOD}).
 * Дальше частица летит по баллистике без запросов к миру — разницы глазом не видно,
 * а проверка коллизий — самая дорогая часть тика частицы.
 */
public abstract class PhysicsParticle extends TextureSheetParticle {
    /** Дальность (в блоках), в пределах которой считаются столкновения. */
    protected static final double PHYSICS_LOD = 24.0;

    /** Доля скорости, сохраняемая при отскоке (0 — прилипает, 1 — упругий мяч). */
    protected float restitution = 0.4f;
    /** Трение о землю после касания. */
    protected float groundFriction = 0.7f;
    protected boolean collides = true;
    protected boolean resting;
    /** Сколько раз частица ударилась — потомки могут гасить вращение и т. п. */
    protected int impacts;

    protected PhysicsParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        super(level, x, y, z, xd, yd, zd);
        this.hasPhysics = false; // столкновения ведём сами
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
    }

    /** Один шаг физики. Вызывать из tick() вместо super.tick(). Возвращает false, если частица умерла. */
    protected boolean stepPhysics() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return false;
        }
        if (resting) {
            return true;
        }
        yd -= 0.04 * gravity;
        boolean near = collides && ParticleBudget.cameraDistSqr(x, y, z) < PHYSICS_LOD * PHYSICS_LOD;
        if (near && xd * xd + yd * yd + zd * zd < 100.0) {
            Vec3 want = new Vec3(xd, yd, zd);
            AABB box = getBoundingBox();
            Vec3 got = Entity.collideBoundingBox(null, want, box, level, List.of());
            setBoundingBox(box.move(got));
            setLocationFromBoundingbox();
            if (got.y != want.y) {
                impacts++;
                if (want.y < 0) onGround = true;
                yd = -yd * restitution;
                xd *= groundFriction;
                zd *= groundFriction;
                if (Math.abs(yd) < 0.015 && want.y < 0) {
                    yd = 0;
                    if (xd * xd + zd * zd < 1.0e-4) resting = true;
                }
            } else {
                onGround = false;
            }
            if (got.x != want.x) {
                xd = -xd * restitution;
                impacts++;
            }
            if (got.z != want.z) {
                zd = -zd * restitution;
                impacts++;
            }
        } else {
            setBoundingBox(getBoundingBox().move(xd, yd, zd));
            setLocationFromBoundingbox();
        }
        xd *= friction;
        yd *= friction;
        zd *= friction;
        return true;
    }
}
