package com.aetherwastes.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.level.Level;

/** Эфирный огонёк: летучий сгусток Эфира, проходит сквозь стены, с него падают Осколки Эфира. */
public class EtherWisp extends Vex {
    public EtherWisp(EntityType<? extends Vex> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10)
                .add(Attributes.ATTACK_DAMAGE, 3)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.MOVEMENT_SPEED, 0.6);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && random.nextInt(3) == 0) {
            level().addParticle(ParticleTypes.WITCH, getRandomX(0.4), getRandomY(), getRandomZ(0.4), 0, 0.02, 0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }
}
