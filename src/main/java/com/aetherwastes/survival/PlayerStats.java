package com.aetherwastes.survival;

import com.aetherwastes.registry.ModAttachments;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/** Доступ к Сосуду и Ясности. */
public final class PlayerStats {
    public static final float VESSEL_MAX = 100f;
    public static final float VESSEL_OVERCHARGE_MAX = 120f;

    private PlayerStats() {}

    public static float vessel(Player p) {
        return p.getData(ModAttachments.VESSEL);
    }

    public static void setVessel(Player p, float v) {
        p.setData(ModAttachments.VESSEL, Mth.clamp(v, 0f, VESSEL_OVERCHARGE_MAX));
    }

    public static float clarity(Player p) {
        return p.getData(ModAttachments.CLARITY);
    }

    public static void setClarity(Player p, float c) {
        p.setData(ModAttachments.CLARITY, Mth.clamp(c, 0f, 100f));
    }
}
