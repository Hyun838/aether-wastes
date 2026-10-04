package com.aetherwastes.entity;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;

/** Солевой призрак: днём бесплотен (ранит только магия), ночью твердеет; вода его растворяет. */
public class SaltWraith extends Zombie {
    public SaltWraith(EntityType<? extends Zombie> type, Level level) {
        super(type, level);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean magic = source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypes.DROWN);
        if (level().isDay() && !magic) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    public boolean isSunSensitive() {
        return false;
    }

    @Override
    public boolean convertsInWater() {
        return false;
    }

    @Override
    public void setBaby(boolean baby) {
        super.setBaby(false);
    }
}
