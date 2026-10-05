package com.aetherwastes.entity.boss;

import com.aetherwastes.entity.SaltWraith;
import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModParticles;
import com.aetherwastes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Лорд Эха — хозяин Склепа Эха. Шагает сквозь тени за спину жертве, поднимает соляных призраков,
 * выпускает волну душ. Во второй фазе становится бесплотным: снаряды проходят сквозь него.
 */
public class EchoLord extends DungeonBoss {
    public EchoLord(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.BLUE);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 280).add(Attributes.ATTACK_DAMAGE, 12).add(Attributes.ARMOR, 10)
                .add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.FOLLOW_RANGE, 40)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    @Override
    public String dungeonKind() {
        return "crypt";
    }

    @Override
    protected void tickAbilities(ServerLevel level, LivingEntity target) {
        int t = abilityTick;
        if (t % 140 == 70) shadowStep(level, target);
        if (t % 420 == 200 && countNearby(SaltWraith.class, 24) < 5) summon(level);
        int novaEvery = enraged() ? 170 : 260;
        if (t % novaEvery == novaEvery - 1) soulNova(level);
        if (tickCount % 4 == 0) {
            level.sendParticles(ModParticles.PHANTOM_WISP.get(), getX(), getY() + 1.2, getZ(), 1, 0.4, 0.8, 0.4, 0.01);
        }
    }

    private void shadowStep(ServerLevel level, LivingEntity target) {
        Vec3 behind = target.position().subtract(target.getLookAngle().multiply(1, 0, 1).normalize().scale(2.5));
        BlockPos at = BlockPos.containing(behind);
        if (!level.noCollision(this, getBoundingBox().move(behind.subtract(position())))) return;
        level.sendParticles(ModParticles.PHANTOM_WISP.get(), getX(), getY() + 1, getZ(), 40, 0.5, 1, 0.5, 0.05);
        teleportTo(behind.x, at.getY(), behind.z);
        lookAt(target, 360, 360);
        level.sendParticles(ModParticles.PHANTOM_WISP.get(), getX(), getY() + 1, getZ(), 40, 0.5, 1, 0.5, 0.05);
        level.playSound(null, blockPosition(), ModSounds.PHANTOM_PHASE.get(), SoundSource.HOSTILE, 1.5f, 0.7f);
        if (target instanceof Player p) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
    }

    private void summon(ServerLevel level) {
        level.playSound(null, blockPosition(), ModSounds.WHISPER.get(), SoundSource.HOSTILE, 2f, 0.6f);
        for (int i = 0; i < (enraged() ? 3 : 2); i++) {
            SaltWraith w = ModEntities.SALT_WRAITH.get().create(level);
            if (w == null) continue;
            double a = random.nextDouble() * Math.PI * 2;
            w.moveTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, random.nextFloat() * 360, 0);
            w.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(w);
            level.sendParticles(ParticleTypes.SOUL, w.getX(), w.getY() + 1, w.getZ(), 20, 0.3, 0.8, 0.3, 0.02);
        }
    }

    private void soulNova(ServerLevel level) {
        Vec3 c = position();
        double r = 7;
        for (int k = 0; k < 30; k += 5) {
            int kk = k;
            schedule(k, () -> telegraph(level, c, r * (kk + 5) / 30.0, ParticleTypes.SOUL_FIRE_FLAME));
        }
        level.playSound(null, blockPosition(), ModSounds.RIFT_OPEN.get(), SoundSource.HOSTILE, 1.5f, 0.6f);
        schedule(32, () -> {
            for (Player p : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(BlockPos.containing(c)).inflate(r),
                    p -> p.distanceToSqr(c) <= r * r && !p.isCreative() && !p.isSpectator())) {
                p.hurt(damageSources().indirectMagic(this, this), 10f);
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
                Vec3 away = p.position().subtract(c).normalize().scale(1.0);
                p.push(away.x, 0.5, away.z);
                p.hurtMarked = true;
            }
            level.sendParticles(ParticleTypes.SOUL, c.x, c.y + 0.5, c.z, 120, r / 2, 0.4, r / 2, 0.05);
            level.sendParticles(ModParticles.PHANTOM_WISP.get(), c.x, c.y + 1, c.z, 80, r / 2, 1, r / 2, 0.08);
            ModParticles.shockwave(level, c.x, c.y + 0.05, c.z, r, 2);
            level.sendParticles(ModParticles.PHANTOM_SHARD.get(), c.x, c.y + 0.6, c.z, 24, r / 4, 0.3, r / 4, 0.3);
            level.playSound(null, BlockPos.containing(c), ModSounds.BOSS_SLAM.get(), SoundSource.HOSTILE, 2f, 1.2f);
        });
    }

    @Override
    protected void onEnrage() {
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0, false, false));
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (enraged() && source.is(DamageTypeTags.IS_PROJECTILE) && random.nextFloat() < 0.5f) {
            if (level() instanceof ServerLevel s) s.sendParticles(ModParticles.PHANTOM_WISP.get(), getX(), getY() + 1, getZ(), 10, 0.3, 0.6, 0.3, 0.02);
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return ModSounds.WHISPER.get();
    }
}
