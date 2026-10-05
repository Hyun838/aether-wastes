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
    public static void clientSetup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            var bow = com.aetherwastes.registry.ModGear.ETHER_BOW.get();
            net.minecraft.client.renderer.item.ItemProperties.register(bow, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pull"),
                    (stack, level, entity, seed) -> entity == null || entity.getUseItem() != stack ? 0f
                            : (stack.getUseDuration(entity) - entity.getUseItemRemainingTicks()) / 20f);
            net.minecraft.client.renderer.item.ItemProperties.register(bow, net.minecraft.resources.ResourceLocation.withDefaultNamespace("pulling"),
                    (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1f : 0f);
        });
    }

    @SubscribeEvent
    public static void registerKeys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(PhaseClient.PHASE_KEY);
    }

    @SubscribeEvent
    public static void registerParticles(net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(com.aetherwastes.registry.ModParticles.ETHER_SPARK.get(), s -> new WastesParticle.Provider(s, WastesParticle.Kind.SPARK));
        event.registerSpriteSet(com.aetherwastes.registry.ModParticles.PHANTOM_WISP.get(), s -> new WastesParticle.Provider(s, WastesParticle.Kind.WISP));
        event.registerSpriteSet(com.aetherwastes.registry.ModParticles.RUNE.get(), s -> new WastesParticle.Provider(s, WastesParticle.Kind.RUNE));
        event.registerSpriteSet(com.aetherwastes.registry.ModParticles.ASH_EMBER.get(), s -> new WastesParticle.Provider(s, WastesParticle.Kind.EMBER));
        event.registerSpriteSet(com.aetherwastes.registry.ModParticles.ETHER_SHARD.get(), s -> new com.aetherwastes.client.fx.ShardParticle.Provider(s, 0.55f, 0.92f, 1f, true));
        event.registerSpriteSet(com.aetherwastes.registry.ModParticles.EMBER_SHARD.get(), s -> new com.aetherwastes.client.fx.ShardParticle.Provider(s, 0.55f, 0.42f, 0.38f, false));
        event.registerSpriteSet(com.aetherwastes.registry.ModParticles.PHANTOM_SHARD.get(), s -> new com.aetherwastes.client.fx.ShardParticle.Provider(s, 0.78f, 0.6f, 1f, true));
        event.registerSpriteSet(com.aetherwastes.registry.ModParticles.SHOCKWAVE.get(), com.aetherwastes.client.fx.ShockwaveParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(AetherWastes.id("ether_hud"), EtherHud::render);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ModModels.SALT_WRAITH, ModModels::saltWraith);
        event.registerLayerDefinition(ModModels.ECHO_LORD, ModModels::echoLord);
        event.registerLayerDefinition(ModModels.ARCHIVE_KEEPER, ModModels::archiveKeeper);
        event.registerLayerDefinition(ModModels.ASH_COLOSSUS, ModModels::ashColossus);
        event.registerLayerDefinition(ModModels.ASH_HOUND, ModModels::ashHound);
        event.registerLayerDefinition(ModModels.ETHER_WISP, ModModels::etherWisp);
        event.registerLayerDefinition(ModModels.SCAR_CRAWLER, ModModels::scarCrawler);
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

        event.registerEntityRenderer(ModEntities.ASH_HOUND.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.ASH_HOUND, Style.QUAD, "ash_hound", 1.0f, 0.5f, false));
        event.registerEntityRenderer(ModEntities.ETHER_WISP.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.ETHER_WISP, Style.FLOAT, "ether_wisp", 0.8f, 0.2f, false));
        event.registerEntityRenderer(ModEntities.SCAR_CRAWLER.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.SCAR_CRAWLER, Style.SPIDER, "scar_crawler", 1.0f, 0.8f, false));

        event.registerEntityRenderer(ModEntities.ECHO_LORD.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.ECHO_LORD, Style.CASTER, "echo_lord", 1.45f, 0.7f, false));
        event.registerEntityRenderer(ModEntities.ARCHIVE_KEEPER.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.ARCHIVE_KEEPER, Style.CASTER, "archive_keeper", 1.45f, 0.7f, false));
        event.registerEntityRenderer(ModEntities.ASH_COLOSSUS.get(),
                ctx -> new SpecRenderer<>(ctx, ModModels.ASH_COLOSSUS, Style.HEAVY, "ash_colossus", 1.55f, 1.2f, false));

        event.registerBlockEntityRenderer(ModBlockEntities.RITUAL_FOCUS.get(), ctx -> new HolderRenderer(ctx, 0.95f, true));
        event.registerBlockEntityRenderer(ModBlockEntities.PURIFYING_OBELISK.get(), com.aetherwastes.client.fx.ArcaneRenderers.Obelisk::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GUARDIAN_SEAL.get(), com.aetherwastes.client.fx.ArcaneRenderers.Seal::new);
        event.registerBlockEntityRenderer(ModBlockEntities.RITUAL_PEDESTAL.get(), ctx -> new HolderRenderer(ctx, 1.1f));
    }
}
