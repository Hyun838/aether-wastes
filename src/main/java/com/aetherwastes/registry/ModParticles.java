package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Собственные частицы: искры Эфира, призрачные огни, руны, пепельные угли. */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, AetherWastes.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ETHER_SPARK = reg("ether_spark");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHANTOM_WISP = reg("phantom_wisp");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> RUNE = reg("rune");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ASH_EMBER = reg("ash_ember");

    // 1.3: объёмные осколки с физикой (октаэдр, кувыркается и отскакивает)
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ETHER_SHARD = reg("ether_shard");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> EMBER_SHARD = reg("ember_shard");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PHANTOM_SHARD = reg("phantom_shard");
    /**
     * Ударная волна по земле. Спавнить с count = 0: скорость X — радиус, Y — палитра
     * (0 эфир, 1 пепел, 2 призрак). См. {@link com.aetherwastes.client.fx.ShockwaveParticle}.
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHOCKWAVE = reg("shockwave");

    /** Ударная волна радиусом radius (только сервер). */
    public static void shockwave(net.minecraft.server.level.ServerLevel level, double x, double y, double z, double radius, int palette) {
        level.sendParticles(SHOCKWAVE.get(), x, y, z, 0, radius, palette, 0, 1.0);
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> reg(String name) {
        return PARTICLES.register(name, () -> new SimpleParticleType(false));
    }

    private ModParticles() {}
}
