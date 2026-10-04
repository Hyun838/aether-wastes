package com.aetherwastes.craft;

import com.aetherwastes.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;

/** Качество изделия из Горна: 0 Грубое, 1 Обычное, 2 Тонкое, 3 Мастерское. */
public final class Quality {
    public static final int CRUDE = 0;
    public static final int COMMON = 1;
    public static final int FINE = 2;
    public static final int MASTER = 3;
    private static final float[] DURABILITY = {0.8f, 1.0f, 1.2f, 1.5f};
    private static final ChatFormatting[] COLORS = {ChatFormatting.GRAY, ChatFormatting.WHITE, ChatFormatting.GREEN, ChatFormatting.GOLD};

    private Quality() {}

    /** -1, если изделие не проходило через Горн. */
    public static int get(ItemStack stack) {
        Integer q = stack.get(ModComponents.QUALITY.get());
        return q == null ? -1 : q;
    }

    public static void set(ItemStack stack, int quality) {
        int old = get(stack);
        float oldFactor = old < 0 ? 1f : DURABILITY[old];
        stack.set(ModComponents.QUALITY.get(), quality);
        Traits.scaleDurability(stack, DURABILITY[quality] / oldFactor);
    }

    /** Слоты гравировки: без Горна 1, затем 1–4 по качеству (+1 за трофей Охотника). */
    public static int engravingSlots(ItemStack stack) {
        int q = get(stack);
        int slots = q < 0 ? 1 : q + 1;
        Boolean extra = stack.get(ModComponents.EXTRA_SLOT.get());
        if (extra != null && extra) slots++;
        return slots;
    }

    public static ChatFormatting color(int q) {
        return COLORS[Math.max(0, Math.min(3, q))];
    }

    /** Качество по жару при извлечении из Горна. */
    public static int fromHeat(float heat, boolean skilled) {
        float w = skilled ? 3f : 0f;
        if (heat >= 65 - w && heat <= 75 + w) return MASTER;
        if (heat >= 55 - w && heat <= 85 + w) return FINE;
        if (heat >= 40 && heat <= 95) return COMMON;
        return CRUDE;
    }
}
