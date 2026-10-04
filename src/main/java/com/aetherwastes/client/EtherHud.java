package com.aetherwastes.client;

import com.aetherwastes.network.ClientStatsCache;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

/** Левый нижний угол: Сосуд, Ясность, давление, температура, Шум, эпоха и травмы. */
public final class EtherHud {
    private static final int BAR_W = 80;
    private static final int BAR_H = 5;

    private EtherHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator() || !ClientStatsCache.received) return;
        if (mc.screen != null) return;

        Font font = mc.font;
        int x = 6;
        int y = g.guiHeight() - 66;

        float vessel = ClientStatsCache.vessel;
        float clarity = ClientStatsCache.clarity;
        float pressure = ClientStatsCache.pressure;
        float temp = ClientStatsCache.temperature;
        float noise = ClientStatsCache.noise;

        bar(g, x, y, Math.min(vessel, 100f) / 100f, 0xFF9B5DE5);
        if (vessel > 100f) g.fill(x, y, x + Math.round(BAR_W * (vessel - 100f) / 100f), y + BAR_H, 0xFFFFD166);
        g.drawString(font, Component.translatable("hud.aetherwastes.vessel", Math.round(vessel)), x + BAR_W + 4, y - 2, 0xFFD0B8FF, true);

        int clarityColor = clarity < 15f ? 0xFFE63946 : clarity < 40f ? 0xFFF4A261 : 0xFF7FD1E8;
        bar(g, x, y + 11, clarity / 100f, clarityColor);
        g.drawString(font, Component.translatable("hud.aetherwastes.clarity", Math.round(clarity)), x + BAR_W + 4, y + 9, 0xFFBFE9F5, true);

        int tColor = temp < 25f ? 0xFF6EC6FF : temp > 75f ? 0xFFFF8C42 : 0xFFA8E6A1;
        bar(g, x, y + 22, temp / 100f, tColor);
        g.drawString(font, Component.translatable("hud.aetherwastes.temperature", Math.round(temp)), x + BAR_W + 4, y + 20, tColor, true);

        int pColor = pressure >= 70f ? 0xFFFF6B6B : pressure <= 20f ? 0xFFAAAAAA : 0xFFE0C3FF;
        Component pText = ClientStatsCache.tide
                ? Component.translatable("hud.aetherwastes.pressure_tide", Math.round(pressure))
                : Component.translatable("hud.aetherwastes.pressure", Math.round(pressure));
        g.drawString(font, pText, x, y + 32, pColor, true);

        int nColor = noise >= 60f ? 0xFFFF6B6B : noise >= 30f ? 0xFFF4A261 : 0xFF999999;
        g.drawString(font, Component.translatable("hud.aetherwastes.noise_era", Math.round(noise),
                Component.translatable("era.aetherwastes.short." + ClientStatsCache.era)), x, y + 42, nColor, true);

        ListTag traumas = ClientStatsCache.journal.getList("traumas", Tag.TAG_STRING);
        if (!traumas.isEmpty()) {
            Component t = Component.empty();
            for (int i = 0; i < traumas.size(); i++) {
                if (i > 0) ((net.minecraft.network.chat.MutableComponent) t).append(", ");
                ((net.minecraft.network.chat.MutableComponent) t).append(Component.translatable("trauma.aetherwastes." + traumas.getString(i)));
            }
            g.drawString(font, t, x, y + 52, 0xFFE63946, true);
        }
    }

    private static void bar(GuiGraphics g, int x, int y, float fraction, int color) {
        g.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, 0xAA000000);
        int w = Math.round(BAR_W * Math.max(0f, Math.min(1f, fraction)));
        if (w > 0) g.fill(x, y, x + w, y + BAR_H, color);
    }
}
