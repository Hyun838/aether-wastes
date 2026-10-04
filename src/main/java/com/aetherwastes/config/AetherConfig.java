package com.aetherwastes.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Настройки мода (config/aetherwastes-common.toml). Всё можно ослабить или выключить. */
public final class AetherConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    static {
        B.push("ether");
    }

    public static final ModConfigSpec.BooleanValue ETHER_TIDES = B
            .comment("Приливы Эфира: каждые 7 игровых дней давление растёт на 2 ночи.")
            .define("etherTides", true);
    public static final ModConfigSpec.DoubleValue PRESSURE_MULTIPLIER = B
            .comment("Множитель природного эфирного давления.")
            .defineInRange("pressureMultiplier", 1.0, 0.0, 3.0);
    public static final ModConfigSpec.DoubleValue VESSEL_REGEN_MULTIPLIER = B
            .comment("Множитель скорости наполнения Сосуда.")
            .defineInRange("vesselRegenMultiplier", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.DoubleValue RING_SCALE = B
            .comment("Масштаб колец мира (1.0: кольца на 1000/3000/6000 блоков от спавна).")
            .defineInRange("ringScale", 1.0, 0.1, 10.0);

    static {
        B.pop().push("survival");
    }

    public static final ModConfigSpec.DoubleValue CLARITY_DRAIN_MULTIPLIER = B
            .comment("Множитель потери Ясности (0 — выключить рассудок).")
            .defineInRange("clarityDrainMultiplier", 1.0, 0.0, 10.0);
    public static final ModConfigSpec.BooleanValue PHANTOM_DAMAGE = B
            .comment("Фантомы при Ясности ниже 15 наносят урон.")
            .define("phantomDamage", true);
    public static final ModConfigSpec.BooleanValue HIGH_PRESSURE_EFFECTS = B
            .comment("Тошнота при высоком давлении и Отдача при переполнении Сосуда.")
            .define("highPressureEffects", true);
    public static final ModConfigSpec.BooleanValue TRAUMAS = B
            .comment("Травмы: переломы, кровотечения, сотрясения, ожоги.")
            .define("traumas", true);
    public static final ModConfigSpec.BooleanValue NUTRITION = B
            .comment("Разнообразие питания.")
            .define("nutrition", true);
    public static final ModConfigSpec.BooleanValue TEMPERATURE = B
            .comment("Температура тела.")
            .define("temperature", true);
    public static final ModConfigSpec.BooleanValue MUTATIONS = B
            .comment("Мутации от долгого пребывания в высоком давлении.")
            .define("mutations", true);

    static {
        B.pop().push("magic");
    }

    public static final ModConfigSpec.BooleanValue FREE_MAGIC = B
            .comment("Разрешить магию с Эпохи I (без ожидания первого Прилива).")
            .define("freeMagic", false);
    public static final ModConfigSpec.BooleanValue DISABLE_ENCHANTING_TABLE = B
            .comment("Отключить ванильный стол зачарования (его заменяет Гравировка).")
            .define("disableEnchantingTable", true);

    static {
        B.pop().push("threats");
    }

    public static final ModConfigSpec.BooleanValue PULSE = B
            .comment("Пульс Мира: Шум, волны мобов, Охотники.")
            .define("pulse", true);
    public static final ModConfigSpec.BooleanValue SIEGES = B
            .comment("Осады Якоря.")
            .define("sieges", true);
    public static final ModConfigSpec.BooleanValue NEMESIS = B
            .comment("Немезида: моб, убивший игрока, запоминает его.")
            .define("nemesis", true);
    public static final ModConfigSpec.BooleanValue ETHER_STORMS = B
            .comment("Эфирные бури с молниями во время Приливов.")
            .define("etherStorms", true);
    public static final ModConfigSpec.DoubleValue SCAR_SPREAD = B
            .comment("Скорость расползания Порчи (0 — не расползается).")
            .defineInRange("scarSpread", 1.0, 0.0, 5.0);
    public static final ModConfigSpec.BooleanValue MIGRATIONS = B
            .comment("Сезонные миграции стай.")
            .define("migrations", true);
    public static final ModConfigSpec.BooleanValue WANDERERS = B
            .comment("Скитальцы приходят сами (иначе — только через ритуал).")
            .define("wanderers", true);
    public static final ModConfigSpec.BooleanValue RIFTS = B
            .comment("Разломы открываются во время Приливов.")
            .define("rifts", true);

    static {
        B.pop().push("base");
    }

    public static final ModConfigSpec.BooleanValue SETTLERS = B
            .comment("Поселенцы приходят к Якорю 3-го уровня и выше.")
            .define("settlers", true);
    public static final ModConfigSpec.BooleanValue UNDERSIDE_DECAY = B
            .comment("Постройки в Изнанке растворяются вне радиуса Якоря.")
            .define("undersideDecay", true);

    static {
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private AetherConfig() {}
}
