package com.aetherwastes.network;

import net.minecraft.nbt.CompoundTag;

/**
 * Последние значения от сервера. Без клиентских импортов,
 * чтобы обработчик пакета безопасно загружался и на выделенном сервере.
 */
public final class ClientStatsCache {
    public static volatile float vessel = 30f;
    public static volatile float clarity = 100f;
    public static volatile float pressure = 50f;
    public static volatile float temperature = 50f;
    public static volatile float noise = 0f;
    public static volatile int era = 1;
    public static volatile boolean tide = false;
    public static volatile boolean received = false;
    /** Призрачный шаг: активен ли (решает сервер), остаток и перезарядка в тиках (отсчитывает клиент). */
    public static volatile boolean phaseActive = false;
    public static volatile int phaseTicks = 0;
    public static volatile int phaseCooldown = 0;
    public static volatile CompoundTag journal = new CompoundTag();

    private ClientStatsCache() {}
}
