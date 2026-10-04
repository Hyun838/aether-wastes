package com.aetherwastes.magic;

import com.aetherwastes.progression.School;
import net.minecraft.ChatFormatting;

/**
 * Все глифы Глифики. Заклинание = Форма + Суть (две — гибрид) + до трёх Модификаторов.
 * «Редкие» глифы нельзя скрафтить — только Озарения, руины и Караваны.
 */
public enum Glyph {
    // Формы
    TOUCH("touch", Type.FORM, 8f, null, false),
    PROJECTILE("projectile", Type.FORM, 12f, null, false),
    SELF("self", Type.FORM, 10f, null, false),
    ZONE("zone", Type.FORM, 20f, null, false),
    WAVE("wave", Type.FORM, 16f, null, true),
    RUNE("rune", Type.FORM, 14f, null, true),
    SUMMON("summon", Type.FORM, 25f, null, true),
    // Сути
    FORGE("forge", Type.ESSENCE, 0f, School.FORGE, false),
    FLOW("flow", Type.ESSENCE, 0f, School.FLOW, false),
    ROOT("root", Type.ESSENCE, 0f, School.ROOT, false),
    SILENCE("silence", Type.ESSENCE, 0f, School.SILENCE, false),
    STONE("stone", Type.ESSENCE, 0f, School.STONE, false),
    ASH("ash", Type.ESSENCE, 0f, School.ASH, false),
    STAR("star", Type.ESSENCE, 0f, School.STAR, false),
    // Модификаторы
    AMPLIFY("amplify", Type.MODIFIER, 0f, null, false),
    EXPAND("expand", Type.MODIFIER, 0f, null, false),
    DELAY("delay", Type.MODIFIER, 0f, null, true),
    CHAIN("chain", Type.MODIFIER, 0f, null, true),
    QUIET("quiet", Type.MODIFIER, 0f, null, true),
    ECHO("echo", Type.MODIFIER, 0f, null, true);

    public enum Type {
        FORM(ChatFormatting.AQUA),
        ESSENCE(ChatFormatting.GOLD),
        MODIFIER(ChatFormatting.LIGHT_PURPLE);

        public final ChatFormatting color;

        Type(ChatFormatting color) {
            this.color = color;
        }

        public String translationKey() {
            return "glyph_type.aetherwastes." + name().toLowerCase();
        }
    }

    private final String id;
    private final Type type;
    private final float baseCost;
    private final School school;
    private final boolean rare;

    Glyph(String id, Type type, float baseCost, School school, boolean rare) {
        this.id = id;
        this.type = type;
        this.baseCost = baseCost;
        this.school = school;
        this.rare = rare;
    }

    public String id() {
        return id;
    }

    public Type type() {
        return type;
    }

    public float baseCost() {
        return baseCost;
    }

    /** Школа для Сутей, иначе null. */
    public School school() {
        return school;
    }

    public boolean rare() {
        return rare;
    }

    public String translationKey() {
        return "glyph.aetherwastes." + id;
    }

    public static Glyph byId(String id) {
        for (Glyph g : values()) if (g.id.equals(id)) return g;
        return null;
    }

    public static Glyph essenceOf(School school) {
        for (Glyph g : values()) if (g.school == school) return g;
        return null;
    }
}
