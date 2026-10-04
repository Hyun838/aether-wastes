package com.aetherwastes.threats;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.Scheduler;
import com.aetherwastes.entity.Wanderer;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/** Скитальцы: приходят сами (ночью, раз в несколько дней) или по ритуалу. Их гибель открывает ворота эпох. */
public final class Wanderers {
    private Wanderers() {}

    public static EntityType<Wanderer> typeOf(String variant) {
        return switch (variant) {
            case EraManager.SPARK -> ModEntities.WANDERER_SPARK.get();
            case EraManager.RESONANCE -> ModEntities.WANDERER_RESONANCE.get();
            case EraManager.CONVERGENCE -> ModEntities.WANDERER_CONVERGENCE.get();
            default -> ModEntities.WANDERER_HEART.get();
        };
    }

    /** Проверка раз в секунду из Пульса. */
    public static void tick(ServerPlayer p, PlayerData d) {
        if (!AetherConfig.WANDERERS.get() || p.level().dimension() != Level.OVERWORLD) return;
        if (p.tickCount % 1200 != 0) return;
        String variant = EraManager.wandererFor(d);
        if (variant == null || p.level().isDay()) return;
        long now = p.level().getGameTime();
        if (now - d.lastWanderer < 48000L) return;
        if (variant.equals(EraManager.SPARK) && d.insights.size() < 5) return;
        if (p.getRandom().nextFloat() > 0.3f) return;
        d.lastWanderer = now;
        Msg.chat(p, ChatFormatting.DARK_PURPLE, "wanderer.aetherwastes.warning",
                Component.translatable("entity.aetherwastes.wanderer_" + variant));
        p.level().playSound(null, p.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.6f, 0.5f);
        Scheduler.after(20 * 60, () -> {
            if (p.isAlive() && !p.hasDisconnected()) spawnNear(p, variant, 28);
        });
    }

    public static Wanderer spawnNear(ServerPlayer p, String variant, int distance) {
        ServerLevel level = p.serverLevel();
        double a = p.getRandom().nextDouble() * Math.PI * 2;
        int x = (int) (p.getX() + Math.cos(a) * distance);
        int z = (int) (p.getZ() + Math.sin(a) * distance);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (Math.abs(y - p.getY()) > 20) y = p.getBlockY();
        return spawnAt(level, new BlockPos(x, y, z), variant, p);
    }

    public static Wanderer spawnAt(ServerLevel level, BlockPos pos, String variant, ServerPlayer target) {
        Wanderer w = typeOf(variant).create(level);
        if (w == null) return null;
        w.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        w.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
        w.addTag("aw_spawned");
        if (target != null) w.setTarget(target);
        level.addFreshEntity(w);
        level.playSound(null, pos, com.aetherwastes.registry.ModSounds.BOSS_ROAR.get(), SoundSource.HOSTILE, 2.5f, 1.0f);
        if (target != null) {
            Msg.title(target, Component.translatable("entity.aetherwastes.wanderer_" + variant).withStyle(ChatFormatting.DARK_PURPLE),
                    Component.translatable("wanderer.aetherwastes.arrived"));
        }
        return w;
    }

    public static void onDefeated(ServerLevel level, Wanderer w) {
        String variant = w.variant();
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, w.getBoundingBox().inflate(64))) {
            PlayerData d = Data.get(p);
            if (d.wandererKills.add(variant)) {
                Msg.chat(p, ChatFormatting.GOLD, "wanderer.aetherwastes.defeated",
                        Component.translatable("entity.aetherwastes.wanderer_" + variant));
            }
            EraManager.check(p);
            Sync.journal(p);
        }
        w.spawnAtLocation(new ItemStack(ModItems.WANDERER_SHARD.get()));
        w.spawnAtLocation(new ItemStack(ModItems.SOUL_ESSENCE.get(), 3));
        if (variant.equals(EraManager.HEART)) {
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, w.getBoundingBox().inflate(64))) {
                com.aetherwastes.world.HeartManager.offerEndings(p);
            }
        }
    }
}
