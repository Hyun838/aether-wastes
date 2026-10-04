package com.aetherwastes.base;

import com.aetherwastes.ether.AnchorData;
import com.aetherwastes.ether.EtherField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * Устройства базы потребляют мощность Якоря. Мощность = уровень × 10, на Эфирной жиле больше.
 * При перегрузке устройства сбоят.
 */
public final class Devices {
    public static final String WARD = "ward";
    public static final String OBELISK = "obelisk";
    public static final String OBSERVATORY = "observatory";
    public static final String ARCHIVE = "archive";
    public static final String FORGE = "forge";

    private Devices() {}

    public static int cost(String type) {
        return switch (type) {
            case WARD -> 2;
            case OBELISK -> 4;
            case OBSERVATORY -> 3;
            case FORGE -> 2;
            default -> 1;
        };
    }

    public static void placed(Level level, BlockPos pos, String type) {
        if (level instanceof ServerLevel server) AnchorData.get(server).putDevice(pos, type);
    }

    public static void removed(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server) AnchorData.get(server).removeDevice(pos);
    }

    public static int capacity(ServerLevel level, EtherField.Anchor anchor) {
        float ley = EtherField.leyStrength(level.getSeed(), anchor.pos().getX(), anchor.pos().getZ());
        return Math.round(anchor.tier() * 10 * (1f + 0.5f * ley));
    }

    public static int load(ServerLevel level, EtherField.Anchor anchor) {
        int r = EtherField.anchorRadius(anchor.tier());
        int total = 0;
        for (Map.Entry<Long, String> e : AnchorData.get(level).devices().entrySet()) {
            if (BlockPos.of(e.getKey()).distSqr(anchor.pos()) <= (double) r * r) total += cost(e.getValue());
        }
        return total;
    }

    /** Устройство в этой точке работает? (Есть Якорь и он не перегружен.) */
    public static boolean powered(ServerLevel level, BlockPos pos) {
        EtherField.Anchor a = EtherField.strongestAnchor(level, pos);
        return a != null && load(level, a) <= capacity(level, a);
    }

    public static boolean nearObelisk(ServerLevel level, BlockPos pos, int radius) {
        for (Map.Entry<Long, String> e : AnchorData.get(level).devices().entrySet()) {
            if (OBELISK.equals(e.getValue()) && BlockPos.of(e.getKey()).distSqr(pos) <= (double) radius * radius) return true;
        }
        return false;
    }
}
