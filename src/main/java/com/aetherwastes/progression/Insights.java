package com.aetherwastes.progression;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.magic.Glyph;
import com.aetherwastes.network.Sync;
import com.aetherwastes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Озарения — заметки из наблюдений за миром. Каждые 5 Озарений одной темы
 * открывают редкий глиф (рецептов у них нет).
 */
public final class Insights {
    public static final String[] THEMES = {"ether", "metal", "herb", "creature", "land"};

    private static final Map<String, List<Glyph>> REWARDS = Map.of(
            "ether", List.of(Glyph.ECHO, Glyph.DELAY),
            "metal", List.of(Glyph.CHAIN, Glyph.AMPLIFY),
            "herb", List.of(Glyph.SUMMON, Glyph.EXPAND),
            "creature", List.of(Glyph.QUIET, Glyph.RUNE),
            "land", List.of(Glyph.WAVE, Glyph.ZONE));

    private Insights() {}

    public static boolean add(ServerPlayer p, String theme, String key) {
        PlayerData d = Data.get(p);
        if (!d.insights.add(theme + ":" + key)) return false;
        p.displayClientMessage(Component.translatable("insight.aetherwastes.gained",
                Component.translatable("insight.aetherwastes.theme." + theme)).withStyle(ChatFormatting.AQUA), true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.5f);

        int count = d.insightCount(theme);
        if (count % 5 == 0) {
            String rewardKey = theme + ":" + count;
            if (d.rewardsGiven.add(rewardKey)) reward(p, d, theme, count);
        }
        EraManager.check(p);
        Sync.journal(p);
        return true;
    }

    private static void reward(ServerPlayer p, PlayerData d, String theme, int count) {
        List<Glyph> pool = REWARDS.get(theme);
        int index = count / 5 - 1;
        ItemStack stack;
        if (pool != null && index < pool.size()) {
            Glyph g = pool.get(index);
            stack = new ItemStack(ModItems.glyph(g));
            Msg.chat(p, ChatFormatting.AQUA, "insight.aetherwastes.reward_glyph",
                    Component.translatable(g.translationKey()), count,
                    Component.translatable("insight.aetherwastes.theme." + theme));
        } else {
            stack = new ItemStack(ModItems.ETHER_SHARD.get(), 3);
            Msg.chat(p, ChatFormatting.AQUA, "insight.aetherwastes.reward_shards", count,
                    Component.translatable("insight.aetherwastes.theme." + theme));
        }
        if (!p.addItem(stack)) p.drop(stack, false);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
    }
}
