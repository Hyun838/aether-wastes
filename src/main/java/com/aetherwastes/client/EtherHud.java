package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.ability.Phase;
import com.aetherwastes.network.ClientStatsCache;
import com.aetherwastes.registry.ModGear;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Интерфейс Пустошей: панель с полосами Сосуда, Ясности и тепла (с иконками и бликом),
 * строка цели, ячейка Призрачного шага у хотбара и призрачная виньетка.
 */
public final class EtherHud {
    private static final int BAR_W = 90;
    private static final int BAR_H = 5;
    private static final int PANEL_W = 142;

    private static ResourceLocation s(String name) {
        return AetherWastes.id("hud/" + name);
    }

    private static final ResourceLocation PANEL = s("panel");
    private static final ResourceLocation BAR_FRAME = s("bar_frame");
    private static final ResourceLocation BAR_VESSEL = s("bar_vessel");
    private static final ResourceLocation BAR_OVERFLOW = s("bar_overflow");
    private static final ResourceLocation BAR_CLARITY = s("bar_clarity");
    private static final ResourceLocation BAR_CLARITY_LOW = s("bar_clarity_low");
    private static final ResourceLocation BAR_WARM = s("bar_warm");
    private static final ResourceLocation BAR_COLD = s("bar_cold");
    private static final ResourceLocation BAR_HOT = s("bar_hot");
    private static final ResourceLocation SHEEN = s("bar_sheen");
    private static final ResourceLocation ICON_VESSEL = s("icon_vessel");
    private static final ResourceLocation ICON_CLARITY = s("icon_clarity");
    private static final ResourceLocation ICON_TEMP = s("icon_temp");
    private static final ResourceLocation ICON_PRESSURE = s("icon_pressure");
    private static final ResourceLocation ICON_NOISE = s("icon_noise");
    private static final ResourceLocation SLOT = s("ability_slot");
    private static final ResourceLocation SLOT_READY = s("ability_slot_ready");
    private static final ResourceLocation PHANTOM_ICON = s("phantom_icon");
    private static final ResourceLocation PHASE_BAR = s("phase_bar");
    private static final ResourceLocation VIGNETTE = AetherWastes.id("textures/gui/phantom_vignette.png");

    private EtherHud() {}

    public static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        float pt = delta.getGameTimeDeltaPartialTick(false);

        renderVignette(g, mc);
        if (mc.player.isSpectator() || !ClientStatsCache.received) return;
        if (mc.screen != null) return;

        Font font = mc.font;
        renderObjective(g, mc, font);
        renderPanel(g, mc, font, pt);
        renderAbility(g, mc, font, pt);
    }

    // ---------------- Виньетка Призрачного шага ----------------
    private static void renderVignette(GuiGraphics g, Minecraft mc) {
        float f = PhaseClient.fade;
        if (f <= 0.01f) return;
        float pulse = 0.85f + 0.15f * Mth.sin((mc.player.tickCount) * 0.15f);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, f * pulse);
        g.blit(VIGNETTE, 0, 0, g.guiWidth(), g.guiHeight(), 0, 0, 256, 256, 256, 256);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    // ---------------- Цель ----------------
    private static void renderObjective(GuiGraphics g, Minecraft mc, Font font) {
        String objKey = ClientStatsCache.journal.getString("objective");
        if (objKey.isEmpty() || mc.getDebugOverlay().showDebugScreen()) return;
        ListTag objArgs = ClientStatsCache.journal.getList("objArgs", Tag.TAG_STRING);
        Object[] a = new Object[objArgs.size()];
        for (int i = 0; i < a.length; i++) a[i] = objArgs.getString(i);
        Component goal = Component.translatable("hud.aetherwastes.goal").withStyle(ChatFormatting.GOLD)
                .append(Component.translatable(objKey, a).withStyle(ChatFormatting.WHITE));
        var lines = font.split(goal, 220);
        int h = 8 + lines.size() * 10;
        RenderSystem.enableBlend();
        g.blitSprite(PANEL, 3, 3, 232, h);
        RenderSystem.disableBlend();
        int gy = 7;
        for (var line : lines) {
            g.drawString(font, line, 9, gy, 0xFFFFFFFF, true);
            gy += 10;
        }
    }

    // ---------------- Панель состояний ----------------
    private static void renderPanel(GuiGraphics g, Minecraft mc, Font font, float pt) {
        ListTag traumas = ClientStatsCache.journal.getList("traumas", Tag.TAG_STRING);
        int rows = 3;
        int panelH = 8 + rows * 12 + 22 + (traumas.isEmpty() ? 0 : 10);
        int x = 4;
        int y = g.guiHeight() - panelH - 4;
        RenderSystem.enableBlend();
        g.blitSprite(PANEL, x, y, PANEL_W, panelH);

        float time = mc.player.tickCount + pt;
        float vessel = ClientStatsCache.vessel;
        float clarity = ClientStatsCache.clarity;
        float temp = ClientStatsCache.temperature;
        float pressure = ClientStatsCache.pressure;
        float noise = ClientStatsCache.noise;

        int rx = x + 5;
        int ry = y + 5;
        // Сосуд
        bar(g, rx, ry, ICON_VESSEL, BAR_VESSEL, Math.min(vessel, 100f) / 100f, time);
        if (vessel > 100f) partial(g, BAR_OVERFLOW, rx + 13, ry + 2, Math.min(1f, (vessel - 100f) / 100f));
        value(g, font, rx, ry, String.valueOf(Math.round(vessel)), 0xFFD9C2FF);
        // Ясность
        ry += 12;
        boolean low = clarity < 25f;
        bar(g, rx, ry, ICON_CLARITY, low ? BAR_CLARITY_LOW : BAR_CLARITY, clarity / 100f, time);
        value(g, font, rx, ry, String.valueOf(Math.round(clarity)), low ? 0xFFFF7A7A : 0xFFBFEFFF);
        // Тепло
        ry += 12;
        ResourceLocation tBar = temp < 25f ? BAR_COLD : temp > 75f ? BAR_HOT : BAR_WARM;
        bar(g, rx, ry, ICON_TEMP, tBar, temp / 100f, time);
        int tColor = temp < 25f ? 0xFF8FD3FF : temp > 75f ? 0xFFFFA060 : 0xFFB8F0B0;
        value(g, font, rx, ry, Math.round(temp) + "°", tColor);
        // Давление, Шум, эпоха
        ry += 13;
        g.blitSprite(ICON_PRESSURE, rx, ry, 9, 9);
        int pColor = pressure >= 70f ? 0xFFFF6B6B : pressure <= 20f ? 0xFFAAAAAA : 0xFFE0C3FF;
        Component pText = ClientStatsCache.tide
                ? Component.translatable("hud.aetherwastes.pressure_tide", Math.round(pressure))
                : Component.translatable("hud.aetherwastes.pressure", Math.round(pressure));
        g.drawString(font, pText, rx + 12, ry + 1, pColor, true);
        ry += 10;
        g.blitSprite(ICON_NOISE, rx, ry, 9, 9);
        int nColor = noise >= 60f ? 0xFFFF6B6B : noise >= 30f ? 0xFFF4A261 : 0xFFA0A0A0;
        g.drawString(font, Component.translatable("hud.aetherwastes.noise_era", Math.round(noise),
                Component.translatable("era.aetherwastes.short." + ClientStatsCache.era)), rx + 12, ry + 1, nColor, true);
        if (!traumas.isEmpty()) {
            ry += 10;
            MutableComponent t = Component.empty();
            for (int i = 0; i < traumas.size(); i++) {
                if (i > 0) t.append(", ");
                t.append(Component.translatable("trauma.aetherwastes." + traumas.getString(i)));
            }
            g.drawString(font, t, rx, ry + 1, 0xFFE63946, true);
        }
        RenderSystem.disableBlend();
    }

    private static void bar(GuiGraphics g, int x, int y, ResourceLocation icon, ResourceLocation fill, float fraction, float time) {
        g.blitSprite(icon, x, y, 9, 9);
        int bx = x + 12;
        g.blitSprite(BAR_FRAME, bx, y + 1, BAR_W + 2, BAR_H + 2);
        float f = Mth.clamp(fraction, 0f, 1f);
        partial(g, fill, bx + 1, y + 2, f);
        int w = Math.round(BAR_W * f);
        if (w > 6) {
            // бегущий блик
            int pos = (int) ((time * 1.6f) % (BAR_W + 40)) - 20;
            int s0 = Math.max(0, pos), s1 = Math.min(w, pos + 12);
            if (s1 > s0) g.blitSprite(SHEEN, 12, BAR_H, s0 - pos, 0, bx + 1 + s0, y + 2, s1 - s0, BAR_H);
        }
    }

    private static void partial(GuiGraphics g, ResourceLocation sprite, int x, int y, float fraction) {
        int w = Math.round(BAR_W * Mth.clamp(fraction, 0f, 1f));
        if (w > 0) g.blitSprite(sprite, BAR_W, BAR_H, 0, 0, x, y, w, BAR_H);
    }

    private static void value(GuiGraphics g, Font font, int x, int y, String text, int color) {
        g.drawString(font, text, x + 12 + BAR_W + 6, y + 1, color, true);
    }

    // ---------------- Ячейка способности ----------------
    public static boolean wearingPhantom(LocalPlayer p) {
        return p.getItemBySlot(EquipmentSlot.HEAD).is(ModGear.PHANTOM_SET[0].get())
                && p.getItemBySlot(EquipmentSlot.CHEST).is(ModGear.PHANTOM_SET[1].get())
                && p.getItemBySlot(EquipmentSlot.LEGS).is(ModGear.PHANTOM_SET[2].get())
                && p.getItemBySlot(EquipmentSlot.FEET).is(ModGear.PHANTOM_SET[3].get());
    }

    private static void renderAbility(GuiGraphics g, Minecraft mc, Font font, float pt) {
        boolean wearing = wearingPhantom(mc.player);
        if (!wearing && !ClientStatsCache.phaseActive) return;
        int x = g.guiWidth() / 2 + 91 + 8;
        int y = g.guiHeight() - 25;
        int cd = ClientStatsCache.phaseCooldown;
        boolean active = ClientStatsCache.phaseActive;
        boolean ready = cd <= 0 && !active;
        RenderSystem.enableBlend();
        g.blitSprite(ready ? SLOT_READY : SLOT, x, y, 24, 24);
        float glow = ready ? 0.75f + 0.25f * Mth.sin((mc.player.tickCount + pt) * 0.2f) : active ? 1f : 0.45f;
        RenderSystem.setShaderColor(glow, glow, glow + (active ? 0.1f : 0f), 1f);
        g.blitSprite(PHANTOM_ICON, x + 3, y + 3, 18, 18);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        if (active) {
            float f = ClientStatsCache.phaseTicks / (float) Phase.DURATION;
            int h = Math.round(18 * f);
            g.blitSprite(PHASE_BAR, 4, 18, 0, 18 - h, x + 26, y + 3 + 18 - h, 4, h);
            String sec = String.valueOf((ClientStatsCache.phaseTicks + 19) / 20);
            g.drawString(font, sec, x + 12 - font.width(sec) / 2, y + 8, 0xFFB8F4FF, true);
        } else if (cd > 0) {
            float f = cd / (float) Phase.COOLDOWN;
            int h = Math.round(18 * f);
            g.fill(x + 3, y + 3 + 18 - h, x + 21, y + 21, 0xA0101018);
            String sec = String.valueOf((cd + 19) / 20);
            g.drawString(font, sec, x + 12 - font.width(sec) / 2, y + 8, 0xFFFFFFFF, true);
        }
        Component key = PhaseClient.PHASE_KEY.getTranslatedKeyMessage();
        String k = key.getString();
        if (k.length() > 3) k = k.substring(0, 3);
        g.drawString(font, k, x + 22 - font.width(k), y - 6, ready ? 0xFFE6FBFF : 0xFF8A8A9A, true);
        RenderSystem.disableBlend();
    }
}
