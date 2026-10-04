package com.aetherwastes.progression;

import net.minecraft.ChatFormatting;

/** Семь магических школ. */
public enum School {
    FORGE("forge", ChatFormatting.GOLD),        // Горнило
    FLOW("flow", ChatFormatting.BLUE),          // Течение
    ROOT("root", ChatFormatting.GREEN),         // Корневище
    SILENCE("silence", ChatFormatting.DARK_GRAY),// Безмолвие
    STONE("stone", ChatFormatting.GRAY),        // Каменный Хор
    ASH("ash", ChatFormatting.DARK_RED),        // Пепельный Завет
    STAR("star", ChatFormatting.AQUA);          // Звёздная Астролябия

    private final String id;
    private final ChatFormatting color;

    School(String id, ChatFormatting color) {
        this.id = id;
        this.color = color;
    }

    public String id() {
        return id;
    }

    public ChatFormatting color() {
        return color;
    }

    public String key() {
        return "school.aetherwastes." + id;
    }

    public static School byId(String id) {
        for (School s : values()) if (s.id.equals(id)) return s;
        return null;
    }

    /** Противоположные школы: свет против тени, жизнь против смерти. */
    public static boolean conflict(School a, School b) {
        return (a == STAR && b == SILENCE) || (a == SILENCE && b == STAR)
                || (a == ROOT && b == ASH) || (a == ASH && b == ROOT);
    }

    /** Имя гибридной Сути для пары школ или null. */
    public static String hybrid(School a, School b) {
        if (a == b) return null;
        int lo = Math.min(a.ordinal(), b.ordinal()), hi = Math.max(a.ordinal(), b.ordinal());
        School x = values()[lo], y = values()[hi];
        if (x == FORGE && y == FLOW) return "steam";
        if (x == ROOT && y == ASH) return "rot";
        if (x == STONE && y == STAR) return "gravity";
        if (x == FORGE && y == STONE) return "magma";
        if (x == FLOW && y == ROOT) return "bloom";
        if (x == SILENCE && y == ASH) return "dread";
        if (x == FORGE && y == STAR) return "flare";
        if (x == FLOW && y == SILENCE) return "mist";
        if (x == ROOT && y == STONE) return "bramble";
        return "unnamed";
    }
}
