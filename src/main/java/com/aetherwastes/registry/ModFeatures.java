package com.aetherwastes.registry;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.world.WastesFeatures;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, AetherWastes.MODID);

    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ARCHIVE_RUINS = FEATURES.register("archive_ruins", WastesFeatures.ArchiveRuins::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ECHO_TOMB = FEATURES.register("echo_tomb", WastesFeatures.EchoTomb::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SUNKEN_OBSERVATORY = FEATURES.register("sunken_observatory", WastesFeatures.SunkenObservatory::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GLASS_SPIKE = FEATURES.register("glass_spike", WastesFeatures.GlassSpike::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> BLEEDING_TREE = FEATURES.register("bleeding_tree", WastesFeatures.BleedingTree::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> SALT_FLAT = FEATURES.register("salt_flat", WastesFeatures.SaltFlat::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GLOWCAP = FEATURES.register("glowcap", WastesFeatures.Glowcap::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> FROZEN_STORM = FEATURES.register("frozen_storm", WastesFeatures.FrozenStorm::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> FLOATING_DEBRIS = FEATURES.register("floating_debris", WastesFeatures.FloatingDebris::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> ASH_PATCH = FEATURES.register("ash_patch", WastesFeatures.AshPatch::new);

    private ModFeatures() {}
}
