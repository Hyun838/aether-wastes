package com.aetherwastes.item;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.magic.Summons;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.School;
import com.aetherwastes.survival.PlayerStats;
import com.aetherwastes.world.Rifts;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Предметы с особым действием: Ключ Изнанки, Печать Второй Эпохи, Эхо Немезиды, трофеи. */
public class SpecialItem extends Item {
    public enum Kind {UNDERSIDE_KEY, SECOND_AGE_SEAL, NEMESIS_ECHO, PLAIN}

    private final Kind kind;
    private final String tooltipKey;

    public SpecialItem(Kind kind, String tooltipKey, Properties properties) {
        super(properties);
        this.kind = kind;
        this.tooltipKey = tooltipKey;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (kind == Kind.PLAIN) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerPlayer p = (ServerPlayer) player;
        switch (kind) {
            case UNDERSIDE_KEY -> {
                if (p.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
                if (Rifts.openTrialNear(p)) p.getCooldowns().addCooldown(this, 20 * 60 * 5);
            }
            case SECOND_AGE_SEAL -> {
                if (!p.isShiftKeyDown()) {
                    Msg.chat(p, ChatFormatting.GOLD, "item.aetherwastes.second_age_seal.confirm");
                    return InteractionResultHolder.fail(stack);
                }
                PlayerData d = Data.get(p);
                d.secondAge++;
                d.era = 1;
                d.schools.clear();
                d.wandererKills.clear();
                d.mutations.clear();
                d.traumas.clear();
                d.visitedUnderside = false;
                d.reconciled = false;
                d.archivist = false;
                d.wasInTide = false;
                PlayerStats.setVessel(p, 30f);
                PlayerStats.setClarity(p, 100f);
                stack.shrink(1);
                Msg.title(p, Component.translatable("second_age.aetherwastes.title").withStyle(ChatFormatting.GOLD),
                        Component.translatable("second_age.aetherwastes.subtitle", d.secondAge + 1));
                level.playSound(null, p.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1f, 1f);
                Sync.all(p);
            }
            case NEMESIS_ECHO -> {
                String name = stack.getHoverName().getString();
                Summons.spawnAlly(p, Component.translatable("entity.aetherwastes.nemesis_echo", name), 20 * 180, School.ASH, School.STONE);
                stack.shrink(1);
            }
            default -> {
            }
        }
        return InteractionResultHolder.success(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kind != Kind.PLAIN;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (tooltipKey != null) tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GRAY));
    }
}
