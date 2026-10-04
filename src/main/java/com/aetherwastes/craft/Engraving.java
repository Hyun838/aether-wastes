package com.aetherwastes.craft;

import com.aetherwastes.block.SlateBlockEntity;
import com.aetherwastes.core.Msg;
import com.aetherwastes.magic.Glyph;
import com.aetherwastes.registry.ModComponents;
import com.aetherwastes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Гравировка заменяет зачарование. Глифы Сутей и Модификаторов со Скрижали вырезаются
 * на оружие, инструмент или броню; число слотов зависит от качества.
 */
public final class Engraving {
    private Engraving() {}

    public static List<String> get(ItemStack stack) {
        List<String> e = stack.get(ModComponents.ENGRAVINGS.get());
        return e == null ? List.of() : e;
    }

    public static void engrave(ServerPlayer p, SlateBlockEntity slate, ItemStack stack) {
        ItemStack off = p.getOffhandItem();
        Boolean extra = stack.get(ModComponents.EXTRA_SLOT.get());
        if (off.is(ModItems.HUNTER_TROPHY.get()) && (extra == null || !extra)) {
            stack.set(ModComponents.EXTRA_SLOT.get(), true);
            off.shrink(1);
            Msg.chat(p, ChatFormatting.GOLD, "engraving.aetherwastes.trophy_slot");
        }

        List<String> current = new ArrayList<>(get(stack));
        int free = Quality.engravingSlots(stack) - current.size();
        if (free <= 0) {
            Msg.bar(p, ChatFormatting.RED, "engraving.aetherwastes.no_slots", Quality.engravingSlots(stack));
            return;
        }
        List<Glyph> taken = new ArrayList<>();
        for (Glyph g : new ArrayList<>(slate.glyphs())) {
            if (taken.size() >= free) break;
            if (g.type() == Glyph.Type.FORM) continue;
            taken.add(g);
        }
        if (taken.isEmpty()) {
            Msg.bar(p, ChatFormatting.GRAY, "engraving.aetherwastes.nothing");
            return;
        }
        for (Glyph g : taken) {
            slate.glyphs().remove(g);
            current.add(g.id());
        }
        slate.setChanged();
        stack.set(ModComponents.ENGRAVINGS.get(), List.copyOf(current));
        p.level().playSound(null, p.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1f, 1.2f);
        Msg.chat(p, ChatFormatting.LIGHT_PURPLE, "engraving.aetherwastes.done", taken.size(),
                Quality.engravingSlots(stack) - current.size());
    }

    private static float amp(List<String> e) {
        int n = 0;
        for (String s : e) if (s.equals("amplify")) n++;
        return 1f + 0.5f * n;
    }

    /** Эффекты гравировки оружия при ударе в ближнем бою. */
    public static void onWeaponHit(ServerPlayer p, LivingEntity target, float damage) {
        List<String> e = get(p.getMainHandItem());
        if (e.isEmpty()) return;
        float f = amp(e);
        var rng = p.getRandom();
        for (String id : e) {
            switch (id) {
                case "forge" -> target.igniteForSeconds(3f * f);
                case "flow" -> p.heal(damage * 0.1f * f);
                case "root" -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(40 * f), 1));
                case "silence" -> {
                    if (rng.nextFloat() < 0.3f) target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                }
                case "stone" -> {
                    Vec3 v = target.position().subtract(p.position()).normalize().scale(0.5 * f);
                    target.push(v.x, 0.15, v.z);
                    target.hurtMarked = true;
                }
                case "ash" -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, Math.round(60 * f), 0));
                case "star" -> {
                    if (rng.nextFloat() < 0.2f) {
                        target.invulnerableTime = 0;
                        target.hurt(p.damageSources().indirectMagic(p, p), 3f * f);
                    }
                }
                default -> {
                }
            }
        }
    }

    /** Сколько надетых частей брони несут гравировку. */
    public static int armorCount(ServerPlayer p, String id) {
        int n = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (get(p.getItemBySlot(slot)).contains(id)) n++;
        }
        return n;
    }

    /** Раз в секунду: пассивные свойства гравированной брони. */
    public static void armorPassives(ServerPlayer p) {
        int flow = armorCount(p, "flow");
        if (flow > 0 && p.isInWaterRainOrBubble() && p.level().getGameTime() % 100 < 20) p.heal(0.5f * flow);
        if (get(p.getItemBySlot(EquipmentSlot.HEAD)).contains("star") && !p.level().isDay()) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, true));
        }
        if (armorCount(p, "ash") > 0 && p.hasEffect(MobEffects.WITHER)) p.removeEffect(MobEffects.WITHER);
        if (armorCount(p, "root") > 0 && p.getFoodData().getSaturationLevel() < 2f && p.level().getGameTime() % 600 < 20) {
            p.getFoodData().eat(1, 0.5f);
        }
    }

    public static Component describe(String id) {
        return Component.translatable("glyph.aetherwastes." + id);
    }
}
