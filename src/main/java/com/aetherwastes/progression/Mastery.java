package com.aetherwastes.progression;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.network.Sync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** Мастерство школ растёт от применения в мире: ковка, рыбалка у воды, урожай, тьма, шахты, охота, звёзды. */
public final class Mastery {
    private Mastery() {}

    public static void add(ServerPlayer p, School school, int xp) {
        PlayerData d = Data.get(p);
        if (!d.knows(school.id()) || xp <= 0) return;
        int before = d.circle(school.id());
        d.masteryXp.merge(school.id(), xp, Integer::sum);
        int after = d.circle(school.id());
        if (after > before) {
            p.displayClientMessage(Component.translatable("mastery.aetherwastes.circle",
                    Component.translatable(school.key()).withStyle(school.color()), after)
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.8f);
            EraManager.check(p);
            Sync.journal(p);
        }
    }

    /** Множитель силы от круга мастерства: +10% за каждый круг после первого. */
    public static float powerBonus(PlayerData d, School s) {
        return 1f + 0.1f * (d.circle(s.id()) - 1);
    }
}
