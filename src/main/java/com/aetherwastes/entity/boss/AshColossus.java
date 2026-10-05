package com.aetherwastes.entity.boss;

import com.aetherwastes.entity.AshHound;
import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModParticles;
import com.aetherwastes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Пепельный Колосс — хозяин Пепельной Цитадели. Медленный и огромный: удар о землю с предупреждением,
 * огненное дыхание, пепельные гончие. Во второй фазе — огненная аура и метеоры.
 */
public class AshColossus extends DungeonBoss {
    public AshColossus(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.RED);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 360).add(Attributes.ATTACK_DAMAGE, 15).add(Attributes.ARMOR, 14)
                .add(Attributes.ARMOR_TOUGHNESS, 4).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40).add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.ATTACK_KNOCKBACK, 1.5);
    }

    @Override
    public String dungeonKind() {
        return "citadel";
    }

    @Override
    protected void tickAbilities(ServerLevel level, LivingEntity target) {
        int t = abilityTick;
        if (t % 160 == 80) slam(level);
        if (t % 120 == 30 && distanceToSqr(target) < 14 * 14) breath(level, target);
        if (t % 420 == 300 && countNearby(AshHound.class, 24) < 4) hounds(level);
        if (enraged()) {
            if (t % 20 == 0) {
                for (Player p : playersAround(3.5)) p.igniteForSeconds(3f);
                level.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 20, 1.2, 1.5, 1.2, 0.02);
            }
            if (t % 200 == 100) meteor(level, target);
        }
        if (tickCount % 3 == 0) {
            level.sendParticles(ModParticles.ASH_EMBER.get(), getX(), getY() + 2.5, getZ(), 2, 0.7, 1.2, 0.7, 0.03);
        }
    }

    private void slam(ServerLevel level) {
        Vec3 c = position();
        double r = 6.5;
        getNavigation().stop();
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 5, false, false));
        for (int k = 0; k < 24; k += 4) schedule(k, () -> telegraph(level, c, r, ModParticles.ASH_EMBER.get()));
        level.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6f, 0.6f);
        schedule(26, () -> {
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(BlockPos.containing(c)).inflate(r),
                    p -> p.distanceToSqr(c) <= r * r && !p.isCreative() && !p.isSpectator())) {
                p.hurt(damageSources().mobAttack(this), 14f);
                Vec3 away = p.position().subtract(c).normalize();
                p.push(away.x * 1.2, 1.0, away.z * 1.2);
                p.hurtMarked = true;
            }
            BlockPos below = BlockPos.containing(c).below();
            level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(below)),
                    c.x, c.y + 0.1, c.z, 200, r / 2, 0.2, r / 2, 0.2);
            level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 6, r / 3, 0.2, r / 3, 0);
            level.sendParticles(ModParticles.ASH_EMBER.get(), c.x, c.y + 0.5, c.z, 80, r / 2, 0.5, r / 2, 0.15);
            level.playSound(null, BlockPos.containing(c), ModSounds.BOSS_SLAM.get(), SoundSource.HOSTILE, 3f, 0.8f);
        });
    }

    private void breath(ServerLevel level, LivingEntity target) {
        level.playSound(null, blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, 0.5f);
        for (int i = 0; i < 10; i++) {
            schedule(i * 2, () -> {
                if (!target.isAlive()) return;
                Vec3 eye = new Vec3(getX(), getY() + 2.8, getZ());
                Vec3 dir = target.getEyePosition().subtract(eye).normalize()
                        .add(random.nextGaussian() * 0.08, random.nextGaussian() * 0.05, random.nextGaussian() * 0.08);
                SmallFireball f = new SmallFireball(level, this, dir.scale(1.2));
                f.setPos(eye.add(dir));
                level.addFreshEntity(f);
            });
        }
    }

    private void hounds(ServerLevel level) {
        for (int i = 0; i < 2; i++) {
            AshHound h = ModEntities.ASH_HOUND.get().create(level);
            if (h == null) continue;
            h.moveTo(getX() + random.nextGaussian() * 2, getY(), getZ() + random.nextGaussian() * 2, 0, 0);
            h.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(h);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, h.getX(), h.getY() + 0.5, h.getZ(), 20, 0.3, 0.3, 0.3, 0.02);
        }
        level.playSound(null, blockPosition(), SoundEvents.WOLF_GROWL, SoundSource.HOSTILE, 2f, 0.5f);
    }

    private void meteor(ServerLevel level, LivingEntity target) {
        Vec3 at = target.position();
        for (int k = 0; k < 30; k += 6) schedule(k, () -> telegraph(level, at, 2.5, ParticleTypes.FLAME));
        schedule(10, () -> {
            LargeFireball f = new LargeFireball(level, this, new Vec3(0, -1.6, 0), 1);
            f.setPos(at.x, at.y + 14, at.z);
            level.addFreshEntity(f);
        });
    }

    @Override
    protected void onEnrage() {
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 600, 0, false, false));
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 600, 0, false, false));
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean r = super.doHurtTarget(target);
        if (r && target instanceof LivingEntity l) l.igniteForSeconds(3f);
        return r;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.5f, 0.6f);
    }
}
