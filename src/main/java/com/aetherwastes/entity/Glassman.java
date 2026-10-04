package com.aetherwastes.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.level.Level;

/** Стеклянник из Стеклянных дюн: отражает снаряды и лучи, но трескается от звука Каменного Хора. */
public class Glassman extends Skeleton {
    public Glassman(EntityType<? extends Skeleton> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            if (source.getEntity() instanceof LivingEntity shooter && shooter != this) {
                shooter.hurt(damageSources().thorns(this), amount * 0.5f);
            }
            if (level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.2, getZ(), 10, 0.3, 0.4, 0.3, 0.05);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isSunBurnTick() {
        return false;
    }
}
