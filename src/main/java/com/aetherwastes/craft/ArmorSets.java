package com.aetherwastes.craft;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.registry.ModGear;
import com.aetherwastes.survival.PlayerStats;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * Бонусы полных комплектов брони.
 * Пепельный — тепло и стойкость рассудка; Эфирная сталь — Сосуд наполняется быстрее, стойкость к отбрасыванию;
 * Призматический — отражает снаряды; Звёздное железо — ночью скорость и сила;
 * Облачение Архивариуса — заклинания дешевле на 30%;
 * Призрачная броня — Призрачный шаг (клавиша V): 10 с полёта сквозь стены, перезарядка 60 с.
 */
public final class ArmorSets {
    public static final String ASHEN = "ashen";
    public static final String ETHER_STEEL = "ether_steel";
    public static final String PRISM = "prism";
    public static final String STAR_IRON = "star_iron";
    public static final String ARCHIVIST = "archivist";
    public static final String PHANTOM = "phantom";

    private ArmorSets() {}

    /** Имя полного комплекта на игроке или null. */
    public static String fullSet(Player p) {
        Holder<ArmorMaterial> mat = null;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = p.getItemBySlot(slot);
            if (!(s.getItem() instanceof ArmorItem armor)) return null;
            if (mat == null) mat = armor.getMaterial();
            else if (!mat.equals(armor.getMaterial())) return null;
        }
        if (mat == null) return null;
        if (mat.is(ModGear.ASHEN.getKey())) return ASHEN;
        if (mat.is(ModGear.ETHER_STEEL.getKey())) return ETHER_STEEL;
        if (mat.is(ModGear.PRISM.getKey())) return PRISM;
        if (mat.is(ModGear.STAR_IRON.getKey())) return STAR_IRON;
        if (mat.is(ModGear.ARCHIVIST.getKey())) return ARCHIVIST;
        if (mat.is(ModGear.PHANTOM.getKey())) return PHANTOM;
        return null;
    }

    public static boolean wearing(Player p, String set) {
        return set.equals(fullSet(p));
    }

    /** Раз в секунду. */
    public static void tick(ServerPlayer p) {
        String set = fullSet(p);
        if (set == null) return;
        PlayerData d = Data.get(p);
        switch (set) {
            case ASHEN -> {
                d.bodyTemp += (50f - d.bodyTemp) * 0.15f;
                PlayerStats.setClarity(p, PlayerStats.clarity(p) + 0.08f);
            }
            case ETHER_STEEL -> PlayerStats.setVessel(p, PlayerStats.vessel(p) + 0.4f);
            case STAR_IRON -> {
                if (!p.level().isDay()) {
                    p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
                }
            }
            case ARCHIVIST -> PlayerStats.setClarity(p, PlayerStats.clarity(p) + 0.05f);
            case PHANTOM -> {
                // Призрак не боится тьмы.
                p.removeEffect(MobEffects.BLINDNESS);
                p.removeEffect(MobEffects.DARKNESS);
                PlayerStats.setClarity(p, PlayerStats.clarity(p) + 0.04f);
            }
            default -> {
            }
        }
    }

    public static float spellCostMultiplier(Player p) {
        return wearing(p, ARCHIVIST) ? 0.7f : 1f;
    }
}
