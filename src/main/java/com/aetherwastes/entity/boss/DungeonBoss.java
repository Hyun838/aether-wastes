package com.aetherwastes.entity.boss;

import com.aetherwastes.core.Data;
import com.aetherwastes.registry.ModParticles;
import com.aetherwastes.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Хозяин подземелья: полоса здоровья, вторая фаза на половине здоровья, привязка к залу,
 * отложенные (с предупреждением) атаки. Наследники описывают способности.
 */
public abstract class DungeonBoss extends Monster {
    protected final ServerBossEvent bossEvent;
    private BlockPos home;
    private boolean enraged;
    private final List<Pending> pending = new ArrayList<>();
    protected int abilityTick;

    private record Pending(int[] ticks, Runnable action) {}

    protected DungeonBoss(EntityType<? extends Monster> type, Level level, BossEvent.BossBarColor color) {
        super(type, level);
        this.bossEvent = new ServerBossEvent(getDisplayName(), color, BossEvent.BossBarOverlay.NOTCHED_10);
        this.bossEvent.setDarkenScreen(true);
        this.xpReward = 250;
        setPersistenceRequired();
    }

    /** Ключ вида подземелья: archive / crypt / citadel. */
    public abstract String dungeonKind();

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
        restrictTo(pos, 22);
    }

    public BlockPos home() {
        return home == null ? blockPosition() : home;
    }

    public boolean enraged() {
        return enraged;
    }

    /** Выполнить действие через delay тиков (для телеграфированных атак). */
    protected void schedule(int delay, Runnable action) {
        pending.add(new Pending(new int[]{delay}, action));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bossEvent.setProgress(getHealth() / getMaxHealth());
        abilityTick++;
        Iterator<Pending> it = pending.iterator();
        List<Runnable> ready = new ArrayList<>();
        while (it.hasNext()) {
            Pending p = it.next();
            if (--p.ticks[0] <= 0) {
                ready.add(p.action);
                it.remove();
            }
        }
        if (isAlive()) ready.forEach(Runnable::run);
        if (!enraged && getHealth() < getMaxHealth() * 0.5f) {
            enraged = true;
            onEnrage();
            ServerLevel level = (ServerLevel) level();
            level.playSound(null, blockPosition(), ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 3f, 0.7f);
            level.sendParticles(ModParticles.RUNE.get(), getX(), getY() + 1, getZ(), 60, 1.5, 1.5, 1.5, 0.1);
            for (ServerPlayer p : bossEvent.getPlayers()) {
                p.displayClientMessage(Component.translatable("boss.aetherwastes.enraged", getDisplayName()).withStyle(ChatFormatting.RED), true);
            }
        }
        // Привязка к залу: слишком далеко — возвращается
        if (home != null && tickCount % 20 == 0 && distanceToSqr(Vec3.atCenterOf(home)) > 30 * 30) {
            teleportTo(home.getX() + 0.5, home.getY() + 1, home.getZ() + 0.5);
            heal(getMaxHealth() * 0.05f);
        }
        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) tickAbilities((ServerLevel) level(), target);
    }

    protected abstract void tickAbilities(ServerLevel level, LivingEntity target);

    protected void onEnrage() {}

    /** Кольцо частиц-предупреждения на земле. */
    protected void telegraph(ServerLevel level, Vec3 at, double radius, ParticleOptions particle) {
        int n = (int) (radius * 10);
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            level.sendParticles(particle, at.x + Math.cos(a) * radius, at.y + 0.15, at.z + Math.sin(a) * radius, 1, 0, 0.02, 0, 0);
        }
    }

    protected List<Player> playersAround(double r) {
        return level().getEntitiesOfClass(Player.class, new AABB(blockPosition()).inflate(r),
                p -> p.isAlive() && !p.isCreative() && !p.isSpectator());
    }

    protected int countNearby(Class<? extends Mob> type, double r) {
        return level().getEntitiesOfClass(type, getBoundingBox().inflate(r)).size();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        bossEvent.setName(getDisplayName());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.IN_WALL) || source.is(DamageTypes.CRAMMING) || source.is(DamageTypes.DROWN)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level from, Level to) {
        return false;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ModParticles.RUNE.get(), getX(), getY() + 1.5, getZ(), 120, 2, 2, 2, 0.1);
            level.sendParticles(ModParticles.ETHER_SPARK.get(), getX(), getY() + 1.5, getZ(), 200, 1.5, 2, 1.5, 0.3);
            for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(40))) {
                if (p instanceof ServerPlayer sp) {
                    Data.get(sp).flags.add("boss_" + dungeonKind());
                    sp.displayClientMessage(Component.translatable("boss.aetherwastes.defeated", getDisplayName()).withStyle(ChatFormatting.GOLD), false);
                }
            }
            bossEvent.removeAllPlayers();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("home", NbtUtils.writeBlockPos(home));
        tag.putBoolean("enraged", enraged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "home").ifPresent(this::setHome);
        enraged = tag.getBoolean("enraged");
        if (hasCustomName()) bossEvent.setName(getDisplayName());
    }
}
