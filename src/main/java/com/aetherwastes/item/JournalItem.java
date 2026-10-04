package com.aetherwastes.item;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.WorldState;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.progression.Insights;
import com.aetherwastes.progression.School;
import com.aetherwastes.survival.Nutrition;
import com.aetherwastes.survival.Traumas;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Журнал Пробуждённого: эпоха, ворота, школы, Озарения, травмы, мутации, Немезида. */
public class JournalItem extends Item {
    public JournalItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            show((ServerPlayer) player);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1f);
        }
        return InteractionResultHolder.success(stack);
    }

    public static void show(ServerPlayer p) {
        PlayerData d = Data.get(p);
        line(p, Component.translatable("journal.aetherwastes.header").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        line(p, Component.translatable("journal.aetherwastes.era", Component.translatable("era.aetherwastes." + d.era))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        line(p, EraManager.gateStatus(d).copy().withStyle(ChatFormatting.GRAY));

        MutableComponent schools = Component.translatable("journal.aetherwastes.schools", d.schools.size(), EraManager.maxSchools(d));
        for (School s : School.values()) {
            if (d.knows(s.id())) {
                schools.append(Component.literal(" "))
                        .append(Component.translatable(s.key()).withStyle(s.color()))
                        .append(Component.literal(" " + roman(d.circle(s.id()))).withStyle(ChatFormatting.GRAY));
            }
        }
        line(p, schools.withStyle(ChatFormatting.WHITE));

        MutableComponent ins = Component.translatable("journal.aetherwastes.insights", d.insights.size());
        for (String t : Insights.THEMES) {
            ins.append(Component.literal(" · ")).append(Component.translatable("insight.aetherwastes.theme." + t))
                    .append(Component.literal(" " + d.insightCount(t)));
        }
        line(p, ins.withStyle(ChatFormatting.AQUA));

        line(p, Component.translatable("journal.aetherwastes.traumas", Traumas.list(d)).withStyle(ChatFormatting.RED));
        if (!d.mutations.isEmpty()) {
            MutableComponent m = Component.translatable("journal.aetherwastes.mutations");
            for (String mu : d.mutations) m.append(" ").append(Component.translatable("mutation.aetherwastes." + mu));
            line(p, m.withStyle(ChatFormatting.DARK_PURPLE));
        }
        line(p, Component.translatable("journal.aetherwastes.food", Nutrition.variety(d), Nutrition.GROUPS.length,
                Component.translatable(Nutrition.malnourished(d) ? "journal.aetherwastes.food.bad" : "journal.aetherwastes.food.ok"))
                .withStyle(ChatFormatting.YELLOW));
        line(p, Component.translatable("journal.aetherwastes.herbs", d.knownHerbs.size()).withStyle(ChatFormatting.GREEN));
        if (d.nemesis.contains("name")) {
            line(p, Component.translatable("journal.aetherwastes.nemesis", d.nemesis.getString("name"), d.nemesis.getInt("level"))
                    .withStyle(ChatFormatting.DARK_RED));
        }
        if (d.reconciled) line(p, Component.translatable("journal.aetherwastes.reconciled").withStyle(ChatFormatting.GOLD));
        if (d.archivist) line(p, Component.translatable("journal.aetherwastes.archivist").withStyle(ChatFormatting.GOLD));
        String ending = WorldState.get(p.getServer()).ending;
        if (!ending.isEmpty()) {
            line(p, Component.translatable("journal.aetherwastes.ending", Component.translatable("ending.aetherwastes." + ending))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (d.secondAge > 0) line(p, Component.translatable("journal.aetherwastes.second_age", d.secondAge).withStyle(ChatFormatting.GOLD));
    }

    private static void line(ServerPlayer p, Component c) {
        p.displayClientMessage(c, false);
    }

    private static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> "V";
        };
    }
}
