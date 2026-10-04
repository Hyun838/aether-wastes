package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.registry.ModEntities;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.SkeletonRenderer;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Zombie;
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
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SALT_WRAITH.get(), ctx -> zombie(ctx, "salt_wraith", 1f));
        event.registerEntityRenderer(ModEntities.RESIN_WALKER.get(), ctx -> zombie(ctx, "resin_walker", 1.08f));
        event.registerEntityRenderer(ModEntities.WANDERER_SPARK.get(), ctx -> zombie(ctx, "wanderer_spark", 1.3f));
        event.registerEntityRenderer(ModEntities.WANDERER_RESONANCE.get(), ctx -> zombie(ctx, "wanderer_resonance", 1.3f));
        event.registerEntityRenderer(ModEntities.WANDERER_CONVERGENCE.get(), ctx -> zombie(ctx, "wanderer_convergence", 1.3f));
        event.registerEntityRenderer(ModEntities.WANDERER_HEART.get(), ctx -> zombie(ctx, "wanderer_heart", 1.45f));
        event.registerEntityRenderer(ModEntities.GLASSMAN.get(), ctx -> new SkeletonRenderer(ctx) {
            private final ResourceLocation tex = AetherWastes.id("textures/entity/glassman.png");

            @Override
            public ResourceLocation getTextureLocation(AbstractSkeleton entity) {
                return tex;
            }
        });
    }

    private static ZombieRenderer zombie(net.minecraft.client.renderer.entity.EntityRendererProvider.Context ctx, String name, float scale) {
        ResourceLocation tex = AetherWastes.id("textures/entity/" + name + ".png");
        return new ZombieRenderer(ctx) {
            @Override
            public ResourceLocation getTextureLocation(Zombie entity) {
                return tex;
            }

            @Override
            protected void scale(Zombie entity, PoseStack poseStack, float partialTick) {
                poseStack.scale(scale, scale, scale);
            }
        };
    }
}
