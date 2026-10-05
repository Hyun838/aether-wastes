package com.aetherwastes.entity.boss;

import com.aetherwastes.entity.EtherWisp;
import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModParticles;
import com.aetherwastes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Хранитель Архива — хозяин Затерянного Архива. Держит дистанцию, бросает самонаводящиеся печати,
 * рисует под игроком руны-мины, призывает огоньки Эфира и уходит в сторону, если его зажали.
 */
public class ArchiveKeeper extends DungeonBoss {
    private int meleeHits;
    private long lastHit;

    public ArchiveKeeper(EntityType<? extends Monster> type, Level level) {
        super(type, level, BossEvent.BossBarColor.PURPLE);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 240).add(Attributes.ATTACK_DAMAGE, 8).add(Attributes.ARMOR, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.KNOCKBACK_RESISTANCE, 0.9).add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    public String dungeonKind() {
        return "archive";
    }

    @Override
    protected void tickAbilities(ServerLevel level, LivingEntity target) {
        int t = abilityTick;
        int boltEvery = enraged() ? 40 : 60;
        if (t % boltEvery == 0 && hasLineOfSight(target)) bolts(level, target, enraged() ? 3 : 1);
        if (t % 200 == 100) runeMines(level, target);
        if (t % 380 == 190 && countNearby(EtherWisp.class, 24) < 4) wisps(level);
        if (enraged() && t % 300 == 150) {
            for (Player p : playersAround(20)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
            level.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 1f, 1.6f);
        }
        if (tickCount % 3 == 0) {
            double a = tickCount * 0.25;
            level.sendParticles(ModParticles.RUNE.get(), getX() + Math.cos(a) * 1.4, getY() + 2.6, getZ() + Math.sin(a) * 1.4, 1, 0, 0, 0, 0);
        }
        // держит дистанцию
        double d = distanceToSqr(target);
        if (d < 25 && t % 20 == 0) {
            Vec3 away = position().subtract(target.position()).normalize().scale(6).add(position());
            getNavigation().moveTo(away.x, away.y, away.z, 1.3);
        }
    }

    private void bolts(ServerLevel level, LivingEntity target, int n) {
        for (int i = 0; i < n; i++) {
            ShulkerBullet b = new ShulkerBullet(level, this, target, Direction.Axis.values()[i % 3]);
            b.setPos(getX(), getY() + 2.4, getZ());
            level.addFreshEntity(b);
        }
        level.playSound(null, blockPosition(), ModSounds.SPELL_CAST.get(), SoundSource.HOSTILE, 1.2f, 0.8f);
    }

    private void runeMines(ServerLevel level, LivingEntity target) {
        Vec3 base = target.position();
        for (int i = 0; i < (enraged() ? 5 : 3); i++) {
            Vec3 at = i == 0 ? base : base.add((random.nextDouble() - 0.5) * 8, 0, (random.nextDouble() - 0.5) * 8);
            for (int k = 0; k < 36; k += 6) schedule(k, () -> telegraph(level, at, 2.0, ModParticles.RUNE.get()));
            schedule(40, () -> {
                level.sendParticles(ModParticles.ETHER_SPARK.get(), at.x, at.y + 0.3, at.z, 60, 0.8, 0.4, 0.8, 0.25);
                level.sendParticles(ParticleTypes.FLASH, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
                level.playSound(null, BlockPos.containing(at), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.5f);
                for (Player p : level.getEntitiesOfClass(Player.class, new AABB(BlockPos.containing(at)).inflate(2.2),
                        p -> !p.isCreative() && !p.isSpectator())) {
                    p.hurt(damageSources().indirectMagic(this, this), 9f);
                    p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 1));
                }
            });
        }
        level.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.5f, 0.9f);
    }

    private void wisps(ServerLevel level) {
        for (int i = 0; i < 3; i++) {
            EtherWisp w = ModEntities.ETHER_WISP.get().create(level);
            if (w == null) continue;
            w.moveTo(getX() + random.nextGaussian() * 2, getY() + 2, getZ() + random.nextGaussian() * 2, 0, 0);
            w.finalizeSpawn(level, level.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            level.addFreshEntity(w);
        }
        level.playSound(null, blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5f, 1.1f);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean r = super.hurt(source, amount);
        if (r && !level().isClientSide && source.getDirectEntity() instanceof Player) {
            long now = level().getGameTime();
            meleeHits = now - lastHit < 60 ? meleeHits + 1 : 1;
            lastHit = now;
            if (meleeHits >= 3) {
                meleeHits = 0;
                blinkAway((ServerLevel) level());
            }
        }
        return r;
    }

    private void blinkAway(ServerLevel level) {
        BlockPos h = home();
        for (int tries = 0; tries < 12; tries++) {
            double x = h.getX() + 0.5 + (random.nextDouble() - 0.5) * 18;
            double z = h.getZ() + 0.5 + (random.nextDouble() - 0.5) * 18;
            double y = h.getY() + 1;
            if (level.noCollision(this, getBoundingBox().move(x - getX(), y - getY(), z - getZ()))) {
                level.sendParticles(ModParticles.RUNE.get(), getX(), getY() + 1, getZ(), 30, 0.5, 1, 0.5, 0.05);
                teleportTo(x, y, z);
                level.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.7f);
                return;
            }
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() {
        return SoundEvents.BOOK_PAGE_TURN;
    }
}
