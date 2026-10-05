package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;

/** Собственная рамка полосы здоровья для хозяев подземелий. */
@EventBusSubscriber(modid = AetherWastes.MODID, value = Dist.CLIENT)
public final class BossBarRenderer {
    private static final ResourceLocation FRAME = AetherWastes.id("hud/boss_frame");
    private static final ResourceLocation BACK = AetherWastes.id("hud/boss_back");

    private BossBarRenderer() {}

    @SubscribeEvent
    public static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Component name = event.getBossEvent().getName();
        String kind = kindOf(name);
        if (kind == null) return;
        event.setCanceled(true);
        event.setIncrement(30);
        GuiGraphics g = event.getGuiGraphics();
        Minecraft mc = Minecraft.getInstance();
        int x = event.getX();
        int y = event.getY() + 6;
        float progress = event.getBossEvent().getProgress();
        RenderSystem.enableBlend();
        g.blitSprite(BACK, x, y, 182, 7);
        int w = Mth.lerpDiscrete(progress, 0, 180);
        if (w > 0) g.blitSprite(AetherWastes.id("hud/boss_fill_" + kind), 180, 5, 0, 0, x + 1, y + 1, w, 5);
        // блик
        int t = (int) ((mc.level == null ? 0 : mc.level.getGameTime()) * 3 % 260) - 40;
        if (t > 0 && t < w) g.fill(x + 1 + t, y + 1, x + 1 + Math.min(w, t + 6), y + 6, 0x40FFFFFF);
        g.blitSprite(FRAME, x - 11, y - 5, 204, 17);
        RenderSystem.disableBlend();
        int tw = mc.font.width(name);
        int color = switch (kind) {
            case "crypt" -> 0xFF9AF0FF;
            case "archive" -> 0xFFD8B8FF;
            default -> 0xFFFFB060;
        };
        g.drawString(mc.font, name, x + 91 - tw / 2, y - 14, color, true);
    }

    private static String kindOf(Component name) {
        if (!(name.getContents() instanceof TranslatableContents tc)) return null;
        return switch (tc.getKey()) {
            case "entity.aetherwastes.echo_lord" -> "crypt";
            case "entity.aetherwastes.archive_keeper" -> "archive";
            case "entity.aetherwastes.ash_colossus" -> "citadel";
            default -> null;
        };
    }
}
