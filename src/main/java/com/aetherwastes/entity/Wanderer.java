package com.aetherwastes.entity;

import com.aetherwastes.progression.EraManager;
import com.aetherwastes.threats.Wanderers;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Скиталец — один из четырёх Архивариусов-боссов.
 * Искра жжёт лучами, Резонанс сотрясает землю, Схождение искривляет пространство,
 * Последний Скиталец у Сердца Раскола владеет всем сразу.
 */
public class Wanderer extends Zombie {
    private final String variant;
    private final ServerBossEvent boss;
    private int cooldown = 60;
    private int cooldown2 = 140;
    private boolean summoned = false;

    public Wanderer(EntityType<? extends Zombie> type, Level level, String variant) {
        super(type, level);
        this.variant = variant;
        this.boss = new ServerBossEvent(Component.translatable("entity.aetherwastes.wanderer_" + variant),
                BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
        this.xpReward = 120;
        setPersistenceRequired();
    }

    public String variant() {
        return variant;
    }

    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        boss.setProgress(getHealth() / getMaxHealth());
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (--cooldown <= 0) {
            cooldown = 70 + random.nextInt(40);
            primary(target);
        }
        if (--cooldown2 <= 0) {
            cooldown2 = 160 + random.nextInt(60);
            secondary(target);
        }
        if (!summoned && getHealth() < getMaxHealth() * 0.5f) {
            summoned = true;
            summonHelpers();
        }
    }

    private void primary(LivingEntity target) {
        ServerLevel level = (ServerLevel) level();
        switch (variant) {
            case EraManager.SPARK -> fireRay(level, target);
            case EraManager.RESONANCE -> quake(level, 8);
            case EraManager.CONVERGENCE -> blink(level, target);
            default -> {
                switch (random.nextInt(3)) {
                    case 0 -> fireRay(level, target);
                    case 1 -> quake(level, 10);
                    default -> blink(level, target);
                }
            }
        }
    }

    private void secondary(LivingEntity target) {
        ServerLevel level = (ServerLevel) level();
        level.playSound(null, blockPosition(), com.aetherwastes.registry.ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 2f, 0.9f + random.nextFloat() * 0.2f);
        switch (variant) {
            case EraManager.SPARK -> {
                for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(6))) p.igniteForSeconds(4f);
                level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 80, 3, 0.5, 3, 0.05);
            }
            case EraManager.RESONANCE -> heal(10f);
            default -> pull(level, 14);
        }
    }

    private void fireRay(ServerLevel level, LivingEntity target) {
        Vec3 from = getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 d = to.subtract(from);
        for (int i = 0; i <= 20; i++) {
            Vec3 p = from.add(d.scale(i / 20.0));
            level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
        }
        target.hurt(damageSources().indirectMagic(this, this), 6f);
        target.igniteForSeconds(4f);
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.5f, 0.7f);
    }

    private void quake(ServerLevel level, double radius) {
        level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 1.5f, 0.6f);
        level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 12, radius / 2, 0.2, radius / 2, 0);
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius))) {
            if (p.onGround()) {
                p.hurt(damageSources().mobAttack(this), 7f);
                p.push(0, 0.7, 0);
                p.hurtMarked = true;
            }
        }
    }

    private void blink(ServerLevel level, LivingEntity target) {
        Vec3 behind = target.position().subtract(target.getLookAngle().multiply(2, 0, 2));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        teleportTo(behind.x, target.getY(), behind.z);
        level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.6f);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
    }

    private void pull(ServerLevel level, double radius) {
        for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(radius))) {
            Vec3 v = position().subtract(p.position()).normalize().scale(1.2);
            p.push(v.x, 0.3, v.z);
            p.hurtMarked = true;
        }
        level.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 1, getZ(), 120, radius / 2, 1, radius / 2, 0.5);
    }

    private void summonHelpers() {
        ServerLevel level = (ServerLevel) level();
        EntityType<? extends Monster> type = switch (variant) {
            case EraManager.SPARK -> EntityType.HUSK;
            case EraManager.RESONANCE -> EntityType.SKELETON;
            case EraManager.CONVERGENCE -> EntityType.ENDERMITE;
            default -> EntityType.VINDICATOR;
        };
        for (int i = 0; i < 3; i++) {
            Monster m = type.create(level);
            if (m == null) continue;
            m.moveTo(getX() + random.nextInt(5) - 2, getY(), getZ() + random.nextInt(5) - 2, 0, 0);
            m.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            m.addTag("aw_spawned");
            if (getTarget() != null) m.setTarget(getTarget());
            level.addFreshEntity(m);
        }
        level.playSound(null, blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.5f, 0.7f);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) Wanderers.onDefeated(level, this);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        boss.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        boss.removePlayer(player);
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

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }
}
