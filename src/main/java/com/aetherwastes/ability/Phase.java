package com.aetherwastes.ability;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.craft.ArmorSets;
import com.aetherwastes.network.ClientStatsCache;
import com.aetherwastes.network.PhaseStatePayload;
import com.aetherwastes.registry.ModParticles;
import com.aetherwastes.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Призрачный шаг — способность полного комплекта Призрачной брони.
 * 10 секунд свободного полёта сквозь стены, затем перезарядка 60 секунд.
 * По окончании игрока бережно выталкивает в ближайшее свободное место.
 */
@EventBusSubscriber(modid = com.aetherwastes.AetherWastes.MODID)
public final class Phase {
    public static final int DURATION = 200;   // 10 c
    public static final int COOLDOWN = 1200;  // 60 c
    private static final float PHASE_FLY_SPEED = 0.065f;

    private Phase() {}

    /** Сейчас ли игрок проходит сквозь стены (работает на обеих сторонах). */
    public static boolean isPhasing(Player p) {
        if (p.level().isClientSide) return p.isLocalPlayer() && ClientStatsCache.phaseActive;
        return Data.get(p).phaseTicks > 0;
    }

    /** Нажатие клавиши способности. */
    public static void request(ServerPlayer p) {
        PlayerData d = Data.get(p);
        if (!ArmorSets.wearing(p, ArmorSets.PHANTOM)) {
            Msg.bar(p, "msg.aetherwastes.phase.need_set");
            return;
        }
        if (d.phaseTicks > 0) {
            end(p, d);   // повторное нажатие — выйти раньше
            return;
        }
        if (d.phaseCooldown > 0 && !p.isCreative()) {
            Msg.bar(p, "msg.aetherwastes.phase.cooldown", (d.phaseCooldown + 19) / 20);
            return;
        }
        start(p, d);
    }

    public static void start(ServerPlayer p, PlayerData d) {
        d.phaseTicks = DURATION;
        d.phaseCooldown = COOLDOWN;
        Abilities ab = p.getAbilities();
        ab.mayfly = true;
        ab.flying = true;
        ab.setFlyingSpeed(PHASE_FLY_SPEED);
        p.onUpdateAbilities();
        p.setForcedPose(net.minecraft.world.entity.Pose.STANDING);
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, DURATION + 10, 0, false, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, DURATION + 10, 0, false, false, false));
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.blockPosition(), ModSounds.PHANTOM_PHASE.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        burst(level, p.position(), 60);
        // Призрак исчезает из поля зрения врагов.
        for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24), m -> m.getTarget() == p)) {
            if (!(m instanceof com.aetherwastes.entity.boss.DungeonBoss)) m.setTarget(null);
        }
        d.flags.add("phase");
        send(p, d);
    }

    public static void end(ServerPlayer p, PlayerData d) {
        d.phaseTicks = 0;
        d.phaseGrace = 40;
        p.noPhysics = false;
        p.setForcedPose(null);
        Abilities ab = p.getAbilities();
        boolean canFly = p.isCreative() || p.isSpectator();
        ab.mayfly = canFly;
        if (!canFly) ab.flying = false;
        ab.setFlyingSpeed(0.05f);
        p.onUpdateAbilities();
        p.removeEffect(MobEffects.INVISIBILITY);
        p.removeEffect(MobEffects.NIGHT_VISION);
        p.fallDistance = 0;
        if (!p.isSpectator()) eject(p);
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 80, 0, false, false, true));
        ServerLevel level = p.serverLevel();
        level.playSound(null, p.blockPosition(), ModSounds.PHANTOM_RETURN.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        burst(level, p.position(), 40);
        send(p, d);
    }

    /** Если игрок застрял в блоках — найти ближайшую свободную точку и перенести туда. */
    public static void eject(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        if (level.noCollision(p, p.getBoundingBox()) && p.getY() > level.getMinBuildHeight()) return;
        Vec3 found = findFree(p, level);
        if (found == null) {
            BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p.blockPosition());
            found = Vec3.atBottomCenterOf(top);
        }
        p.teleportTo(found.x, found.y, found.z);
        p.setDeltaMovement(Vec3.ZERO);
    }

    private static Vec3 findFree(ServerPlayer p, ServerLevel level) {
        BlockPos base = p.blockPosition();
        AABB box = p.getBoundingBox();
        // Сначала ищем ближайшие клетки по расстоянию (оболочками), предпочитая точки с опорой под ногами.
        Vec3 fallback = null;
        for (int r = 0; r <= 10; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != r) continue;
                        BlockPos c = base.offset(dx, dy, dz);
                        if (c.getY() <= level.getMinBuildHeight() || c.getY() >= level.getMaxBuildHeight() - 2) continue;
                        Vec3 at = new Vec3(c.getX() + 0.5, c.getY(), c.getZ() + 0.5);
                        AABB moved = box.move(at.x - p.getX(), at.y - p.getY(), at.z - p.getZ());
                        if (!level.noCollision(p, moved)) continue;
                        if (!level.getBlockState(c.below()).getCollisionShape(level, c.below()).isEmpty()) return at;
                        if (fallback == null) fallback = at;
                    }
                }
            }
            if (fallback != null && r >= 3) return fallback;
        }
        return fallback;
    }

    private static void burst(ServerLevel level, Vec3 at, int n) {
        level.sendParticles(ModParticles.PHANTOM_WISP.get(), at.x, at.y + 1, at.z, n, 0.5, 0.9, 0.5, 0.04);
    }

    public static void send(ServerPlayer p, PlayerData d) {
        try {
            if (p.connection == null || !p.connection.hasChannel(PhaseStatePayload.TYPE)) return;
            PacketDistributor.sendToPlayer(p, new PhaseStatePayload(d.phaseTicks, d.phaseCooldown));
        } catch (RuntimeException ignored) {
        }
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        PlayerData d = Data.get(p);
        if (d.phaseGrace > 0) d.phaseGrace--;
        if (d.phaseCooldown > 0 && d.phaseTicks <= 0) {
            d.phaseCooldown--;
            if (d.phaseCooldown == 0) {
                send(p, d);
                if (ArmorSets.wearing(p, ArmorSets.PHANTOM)) {
                    Msg.bar(p, "msg.aetherwastes.phase.ready");
                    p.serverLevel().playSound(null, p.blockPosition(), ModSounds.WHISPER.get(), SoundSource.PLAYERS, 0.5f, 1.6f);
                }
            }
        }
        if (d.phaseTicks <= 0) return;
        if (!p.isAlive() || !ArmorSets.wearing(p, ArmorSets.PHANTOM) || p.isSpectator()) {
            end(p, d);
            return;
        }
        d.phaseTicks--;
        p.noPhysics = true;
        p.fallDistance = 0;
        Abilities ab = p.getAbilities();
        if (!ab.mayfly || !ab.flying) {
            ab.mayfly = true;
            ab.flying = true;
            p.onUpdateAbilities();
        }
        if (p.getY() < p.level().getMinBuildHeight() + 1) {
            p.teleportTo(p.getX(), p.level().getMinBuildHeight() + 1, p.getZ());
        }
        ServerLevel level = p.serverLevel();
        if (p.tickCount % 2 == 0) {
            level.sendParticles(ModParticles.PHANTOM_WISP.get(), p.getX(), p.getY() + 1, p.getZ(), 2, 0.35, 0.7, 0.35, 0.01);
        }
        if (d.phaseTicks == 60 || d.phaseTicks == 40 || d.phaseTicks == 20) {
            level.playSound(null, p.blockPosition(), ModSounds.WHISPER.get(), SoundSource.PLAYERS, 0.6f, 1.8f);
            Msg.bar(p, "msg.aetherwastes.phase.ending", d.phaseTicks / 20);
        }
        if (d.phaseTicks == 0) end(p, d);
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        PlayerData d = Data.get(p);
        if (d.phaseTicks <= 0 && d.phaseGrace <= 0) return;
        var src = event.getSource();
        if (src.is(DamageTypes.IN_WALL) || src.is(DamageTypes.CRAMMING) || src.is(DamageTypeTags.IS_FALL)) {
            event.setCanceled(true);
        } else if (d.phaseTicks > 0 && src.is(DamageTypeTags.IS_PROJECTILE)) {
            event.setCanceled(true);   // стрелы проходят сквозь призрака
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PlayerData d = Data.get(p);
            if (d.phaseTicks > 0) end(p, d);
            else send(p, d);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PlayerData d = Data.get(p);
            d.phaseTicks = 0;
            send(p, d);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            PlayerData d = Data.get(p);
            if (d.phaseTicks > 0) end(p, d);
        }
    }
}
