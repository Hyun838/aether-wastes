package com.aetherwastes.progression;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.network.Sync;
import com.aetherwastes.world.HeartManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Пять эпох и ворота между ними.
 * I→II: пережить первый Прилив. II→III: Скиталец Искры + 15 Озарений.
 * III→IV: Скиталец Резонанса + побывать в Изнанке. IV→V: Скиталец Схождения + 5-й круг в любой школе.
 */
public final class EraManager {
    public static final String SPARK = "spark";
    public static final String RESONANCE = "resonance";
    public static final String CONVERGENCE = "convergence";
    public static final String HEART = "heart";

    private EraManager() {}

    public static int maxSchools(PlayerData d) {
        if (d.archivist) return School.values().length;
        return switch (d.era) {
            case 1 -> AetherConfig.FREE_MAGIC.get() ? 1 : 0;
            case 2 -> 1;
            case 3 -> 2;
            case 4 -> 4;
            default -> School.values().length;
        };
    }

    public static int maxAnchorTier(PlayerData d) {
        return switch (d.era) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            default -> 5;
        };
    }

    /** Вызывается раз в секунду и после важных событий. */
    public static void check(ServerPlayer p) {
        PlayerData d = Data.get(p);
        if (d.era == 2 && d.wandererKills.contains(SPARK) && d.insights.size() >= 15) {
            advance(p, 3);
        } else if (d.era == 3 && d.wandererKills.contains(RESONANCE) && d.visitedUnderside) {
            advance(p, 4);
        } else if (d.era == 4 && d.wandererKills.contains(CONVERGENCE) && maxCircle(d) >= 5) {
            advance(p, 5);
        }
    }

    /** Ворота I→II: Прилив закончился, а игрок жив. */
    public static void onTideState(ServerPlayer p, boolean tide) {
        PlayerData d = Data.get(p);
        if (d.wasInTide && !tide) {
            Insights.add(p, "ether", "tide_" + (p.level().getDayTime() / 24000L));
            if (d.era == 1) advance(p, 2);
        }
        d.wasInTide = tide;
    }

    public static int maxCircle(PlayerData d) {
        int best = 0;
        for (School s : School.values()) {
            if (d.knows(s.id())) best = Math.max(best, d.circle(s.id()));
        }
        return best;
    }

    public static void advance(ServerPlayer p, int era) {
        PlayerData d = Data.get(p);
        if (era <= d.era) return;
        d.era = era;
        Msg.title(p,
                Component.translatable("era.aetherwastes." + era).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("era.aetherwastes.reached").withStyle(ChatFormatting.GRAY));
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        Msg.chat(p, ChatFormatting.LIGHT_PURPLE, "era.aetherwastes." + era + ".unlocks");
        Msg.chat(p, ChatFormatting.GRAY, "era.aetherwastes." + era + ".gate");
        if (era == 5) {
            HeartManager.announce(p);
        }
        Sync.all(p);
    }

    /** Подсказка: что нужно для следующих ворот. */
    public static Component gateStatus(PlayerData d) {
        return switch (d.era) {
            case 1 -> Component.translatable("gate.aetherwastes.1");
            case 2 -> Component.translatable("gate.aetherwastes.2",
                    mark(d.wandererKills.contains(SPARK)), Math.min(15, d.insights.size()));
            case 3 -> Component.translatable("gate.aetherwastes.3",
                    mark(d.wandererKills.contains(RESONANCE)), mark(d.visitedUnderside));
            case 4 -> Component.translatable("gate.aetherwastes.4",
                    mark(d.wandererKills.contains(CONVERGENCE)), maxCircle(d));
            default -> Component.translatable("gate.aetherwastes.5", mark(d.wandererKills.contains(HEART)));
        };
    }

    private static String mark(boolean done) {
        return done ? "✔" : "✘";
    }

    /** Вариант Скитальца для текущей эпохи игрока или null. */
    public static String wandererFor(PlayerData d) {
        return switch (d.era) {
            case 2 -> d.wandererKills.contains(SPARK) ? null : SPARK;
            case 3 -> d.wandererKills.contains(RESONANCE) ? null : RESONANCE;
            case 4 -> d.wandererKills.contains(CONVERGENCE) ? null : CONVERGENCE;
            default -> null;
        };
    }
}
