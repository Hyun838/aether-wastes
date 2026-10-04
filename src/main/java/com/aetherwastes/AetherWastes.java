package com.aetherwastes;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.network.ModNetwork;
import com.aetherwastes.registry.ModAttachments;
import com.aetherwastes.registry.ModBlockEntities;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.registry.ModComponents;
import com.aetherwastes.registry.ModCreativeTabs;
import com.aetherwastes.registry.ModEntities;
import com.aetherwastes.registry.ModFeatures;
import com.aetherwastes.registry.ModItems;
import com.aetherwastes.registry.ModRecipes;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * «Эфирные Пустоши» — глубокое выживание и магия Эфира.
 * Пять эпох, семь школ, Глифика, Горн, ритуалы, Пульс Мира, Изнанка и Сердце Раскола.
 */
@Mod(AetherWastes.MODID)
public class AetherWastes {
    public static final String MODID = "aetherwastes";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AetherWastes(IEventBus modEventBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modEventBus);
        com.aetherwastes.registry.ModGear.init();
        ModItems.ITEMS.register(modEventBus);
        com.aetherwastes.registry.ModGear.ARMOR_MATERIALS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModComponents.COMPONENTS.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModFeatures.FEATURES.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        com.aetherwastes.registry.ModSounds.SOUNDS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modEventBus.addListener(ModNetwork::register);
        container.registerConfig(ModConfig.Type.COMMON, AetherConfig.SPEC);
        LOGGER.info("Aether Wastes: Эфир пробуждается.");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
