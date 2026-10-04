package com.aetherwastes.craft;

import com.aetherwastes.ether.EtherField;
import com.aetherwastes.registry.ModComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Свойства материалов. У каждого металла в каждом мире 1–3 свойства, заданных сидом.
 * Изделия из металла получают его свойства (проверка инвентаря раз в 5 секунд).
 */
public final class Traits {
    public static final String DURABLE = "durable";
    public static final String BRITTLE = "brittle";
    public static final String SHARP = "sharp";
    public static final String LIGHT = "light";
    public static final String WARM = "warm";
    public static final String RESONANT = "resonant";
    public static final String CONDUCTIVE = "conductive";
    public static final String[] ALL = {DURABLE, BRITTLE, SHARP, LIGHT, WARM, RESONANT, CONDUCTIVE};

    public static final String[] METALS = {"iron", "gold", "copper", "netherite", "star_iron"};

    private Traits() {}

    public static List<String> traitsOf(long seed, String metal) {
        Random r = new Random(seed ^ (metal.hashCode() * 0x5851F42D4C957F2DL));
        int n = 1 + r.nextInt(3);
        List<String> out = new ArrayList<>();
        while (out.size() < n) {
            String t = ALL[r.nextInt(ALL.length)];
            if (out.contains(t)) continue;
            if ((t.equals(DURABLE) && out.contains(BRITTLE)) || (t.equals(BRITTLE) && out.contains(DURABLE))) continue;
            out.add(t);
        }
        return out;
    }

    /** Металл изделия по его названию, или null. */
    public static String metalOf(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (id.startsWith("iron_") || id.startsWith("chainmail_")) return "iron";
        if (id.startsWith("golden_")) return "gold";
        if (id.startsWith("netherite_")) return "netherite";
        if (id.startsWith("copper_")) return "copper";
        return null;
    }

    public static List<String> get(ItemStack stack) {
        List<String> t = stack.get(ModComponents.TRAITS.get());
        return t == null ? List.of() : t;
    }

    public static int has(ItemStack stack, String trait) {
        return get(stack).contains(trait) ? 1 : 0;
    }

    /** Сколько надетых и держимых предметов имеют свойство. */
    public static int count(ServerPlayer p, String trait) {
        int n = has(p.getMainHandItem(), trait);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            n += has(p.getItemBySlot(slot), trait);
        }
        return n;
    }

    public static void add(ItemStack stack, String trait) {
        List<String> list = new ArrayList<>(get(stack));
        if (list.contains(trait)) return;
        list.add(trait);
        stack.set(ModComponents.TRAITS.get(), List.copyOf(list));
        if (trait.equals(DURABLE)) scaleDurability(stack, 1.3f);
        if (trait.equals(BRITTLE)) scaleDurability(stack, 0.75f);
    }

    public static void scaleDurability(ItemStack stack, float factor) {
        if (!stack.isDamageableItem()) return;
        int max = Math.max(1, Math.round(stack.getMaxDamage() * factor));
        stack.set(DataComponents.MAX_DAMAGE, max);
        if (stack.getDamageValue() >= max) stack.setDamageValue(max - 1);
    }

    /** Случайное свойство (для бурь, Приливов и ритуалов). */
    public static String random(net.minecraft.util.RandomSource rng) {
        return ALL[rng.nextInt(ALL.length)];
    }

    /** Металлические изделия без свойств получают свойства своего металла. */
    public static void scanInventory(ServerPlayer p) {
        long seed = p.serverLevel().getServer().overworld().getSeed();
        for (ItemStack s : p.getInventory().items) apply(s, seed);
        for (ItemStack s : p.getInventory().armor) apply(s, seed);
        apply(p.getOffhandItem(), seed);
    }

    private static void apply(ItemStack s, long seed) {
        if (s.isEmpty() || !s.isDamageableItem() || s.has(ModComponents.TRAITS.get())) return;
        String metal = metalOf(s);
        if (metal == null) return;
        List<String> traits = traitsOf(seed, metal);
        s.set(ModComponents.TRAITS.get(), List.copyOf(traits));
        if (traits.contains(DURABLE)) scaleDurability(s, 1.3f);
        if (traits.contains(BRITTLE)) scaleDurability(s, 0.75f);
    }

    /** Для чисел в подсказках. */
    public static double hashFor(long seed, String metal) {
        return EtherField.hash(seed, metal.hashCode(), 7);
    }
}
