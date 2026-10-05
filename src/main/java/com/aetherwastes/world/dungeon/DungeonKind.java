package com.aetherwastes.world.dungeon;

import com.aetherwastes.AetherWastes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Три вида подземелий.
 * Затерянный Архив — глубоко под землёй, книги, руны и Хранитель Архива.
 * Склеп Эха — под лесами и равнинами, гробницы и Лорд Эха (Призрачная броня).
 * Пепельная Цитадель — крепость на поверхности сухих земель и Пепельный Колосс.
 */
public enum DungeonKind implements StringRepresentable {
    ARCHIVE("archive", 5, 5, false),
    CRYPT("crypt", 5, 6, false),
    CITADEL("citadel", 5, 5, true);

    public static final com.mojang.serialization.Codec<DungeonKind> CODEC = StringRepresentable.fromEnum(DungeonKind::values);

    private final String name;
    public final int width;
    public final int depth;
    public final boolean surface;

    DungeonKind(String name, int width, int depth, boolean surface) {
        this.name = name;
        this.width = width;
        this.depth = depth;
        this.surface = surface;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public ResourceKey<LootTable> loot(String type) {
        return ResourceKey.create(Registries.LOOT_TABLE, AetherWastes.id("chests/dungeon/" + name + "_" + type));
    }

    public static DungeonKind byId(int id) {
        DungeonKind[] v = values();
        return v[Math.floorMod(id, v.length)];
    }
}
