package com.aetherwastes.client.fx;

import com.aetherwastes.AetherWastes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * OBJ-модели (загрузчик neoforge:obj), которые рисуются блок-сущностями: кристалл,
 * кольцо рун, сфера печати. Модели запекаются один раз при загрузке ресурсов,
 * в кадре только выводятся готовые квады — без разбора геометрии и аллокаций.
 *
 * <p>Здесь же — мягкая «тень»-пятно под парящими объектами (ванильная текстура
 * тени сущностей): в обычном рендере Minecraft у блок-сущностей теней нет,
 * а с шейдерпаком (Iris/Oculus) эти модели отбрасывают настоящие тени сами.
 */
@EventBusSubscriber(modid = AetherWastes.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ArcaneModels {
    public static final ModelResourceLocation CRYSTAL = mrl("ether_crystal");
    public static final ModelResourceLocation RUNE_RING = mrl("rune_ring");
    public static final ModelResourceLocation SEAL_ORB = mrl("seal_orb");

    private static final RenderType SHADOW = RenderType.entityShadow(ResourceLocation.withDefaultNamespace("textures/misc/shadow.png"));

    private ArcaneModels() {}

    private static ModelResourceLocation mrl(String name) {
        return ModelResourceLocation.standalone(AetherWastes.id("block/obj/" + name));
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        event.register(CRYSTAL);
        event.register(RUNE_RING);
        event.register(SEAL_ORB);
    }

    /** Нарисовать запечённую модель с тинтом. Свет — упакованный lightmap (FULL_BRIGHT для свечения). */
    public static void render(PoseStack pose, MultiBufferSource buffers, ModelResourceLocation id, float r, float g, float b, int light) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
        RenderType type = Sheets.translucentCullBlockSheet();
        VertexConsumer vc = buffers.getBuffer(type);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer()
                .renderModel(pose.last(), vc, null, model, r, g, b, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, type);
    }

    /** Мягкое круглое пятно тени на высоте y (в координатах блока), ослабевает с высотой объекта. */
    public static void blobShadow(PoseStack pose, MultiBufferSource buffers, float y, float radius, float alpha) {
        VertexConsumer vc = buffers.getBuffer(SHADOW);
        PoseStack.Pose p = pose.last();
        float x0 = 0.5f - radius, x1 = 0.5f + radius, z0 = 0.5f - radius, z1 = 0.5f + radius;
        shadowVertex(vc, p, x0, y, z0, 0, 0, alpha);
        shadowVertex(vc, p, x0, y, z1, 0, 1, alpha);
        shadowVertex(vc, p, x1, y, z1, 1, 1, alpha);
        shadowVertex(vc, p, x1, y, z0, 1, 0, alpha);
    }

    private static void shadowVertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, float a) {
        vc.addVertex(p, x, y, z).setColor(1f, 1f, 1f, a).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(p, 0f, 1f, 0f);
    }

    /** Пульсация яркости 0..1 с фазой от позиции блока, чтобы соседние объекты мерцали вразнобой. */
    public static float pulse(float time, long seed, float speed) {
        return 0.5f + 0.5f * (float) Math.sin(time * speed + (seed & 255) * 0.37f);
    }
}
