package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.client.AnimatedModel.Style;
import com.aetherwastes.registry.ModBlockEntities;
import com.aetherwastes.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

@EventBusSubscriber(modid = AetherWastes.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(AetherWastes.id("ether_hud"), EtherHud::render);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModels.SALT_WRAITH, ModModels::saltWraith);
        event.registerLayerDefinition(ModModels.RESIN_WALKER, ModModels::resinWalker);
        event.registerLayerDefinition(ModModels.GLASSMAN, ModModels::glassman);
        event.registerLayerDefinition(ModModels.WANDERER_SPARK, ModModels::wandererSpark);
        event.registerLayerDefinition(ModModels.WANDERER_RESONANCE, ModModels::wandererResonance);
        event.registerLayerDefinition(ModModels.WANDERER_CONVERGENCE, ModModels::wandererConvergence);
        event.registerLayerDefinition(ModModels.WANDERER_HEART, ModModels::wandererHeart);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SALT_WRAITH.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.SALT_WRAITH, Style.FLOAT, "salt_wraith", 1.0f, 0.4f, false));
        event.registerEntityRenderer(ModEntities.RESIN_WALKER.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.RESIN_WALKER, Style.HEAVY, "resin_walker", 1.05f, 0.6f, false));
        event.registerEntityRenderer(ModEntities.GLASSMAN.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.GLASSMAN, Style.BIPED, "glassman", 1.0f, 0.4f, true));
        event.registerEntityRenderer(ModEntities.WANDERER_SPARK.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.WANDERER_SPARK, Style.CASTER, "wanderer_spark", 1.3f, 0.6f, false));
        event.registerEntityRenderer(ModEntities.WANDERER_RESONANCE.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.WANDERER_RESONANCE, Style.CASTER, "wanderer_resonance", 1.35f, 0.6f, false));
        event.registerEntityRenderer(ModEntities.WANDERER_CONVERGENCE.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.WANDERER_CONVERGENCE, Style.CASTER, "wanderer_convergence", 1.3f, 0.6f, false));
        event.registerEntityRenderer(ModEntities.WANDERER_HEART.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.WANDERER_HEART, Style.CASTER, "wanderer_heart", 1.45f, 0.7f, false));

        event.registerBlockEntityRenderer(ModBlockEntities.RITUAL_FOCUS.get(), ctx -> new HolderRenderer(ctx, 0.95f));
        event.registerBlockEntityRenderer(ModBlockEntities.RITUAL_PEDESTAL.get(), ctx -> new HolderRenderer(ctx, 1.1f));
    }
}
