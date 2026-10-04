package com.aetherwastes.core;

import com.aetherwastes.AetherWastes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** Измерения мода. */
public final class Dims {
    public static final ResourceKey<Level> UNDERSIDE = ResourceKey.create(Registries.DIMENSION, AetherWastes.id("underside"));

    private Dims() {}

    public static boolean isUnderside(Level level) {
        return level.dimension() == UNDERSIDE;
    }
}
