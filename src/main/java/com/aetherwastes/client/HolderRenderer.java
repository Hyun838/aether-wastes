package com.aetherwastes.client;

import com.aetherwastes.block.HolderBlockEntity;
import com.aetherwastes.client.fx.ArcaneModels;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Предмет на ритуальном фокусе или постаменте парит и медленно вращается.
 * Над фокусом вращается OBJ-кольцо рун: тусклое, пока фокус пуст, яркое с предметом.
 */
public class HolderRenderer implements BlockEntityRenderer<HolderBlockEntity> {
    private final float height;
    private final boolean ring;

    public HolderRenderer(BlockEntityRendererProvider.Context ctx, float height) {
        this(ctx, height, false);
    }

    public HolderRenderer(BlockEntityRendererProvider.Context ctx, float height, boolean ring) {
        this.height = height;
        this.ring = ring;
    }

    @Override
    public void render(HolderBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (be.getLevel() == null) return;
        ItemStack stack = be.item();
        float time = (be.getLevel().getGameTime() % 24000L) + partialTick;
        if (ring) renderRing(be, stack.isEmpty(), time, pose, buffers);
        if (stack.isEmpty()) return;
        pose.pushPose();
        pose.translate(0.5, height + 0.08 * Math.sin(time / 12.0), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(time * 2f));
        pose.scale(0.6f, 0.6f, 0.6f);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, buffers, be.getLevel(), (int) be.getBlockPos().asLong());
        pose.popPose();
    }

    private void renderRing(HolderBlockEntity be, boolean empty, float time, PoseStack pose, MultiBufferSource buffers) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if (be.getBlockPos().getCenter().distanceToSqr(cam) > 32 * 32) return; // LOD: деталь ближнего плана
        float k = empty ? 0.45f : 0.8f + 0.2f * ArcaneModels.pulse(time, be.getBlockPos().asLong(), 0.15f);
        pose.pushPose();
        pose.translate(0.5, height + 0.05, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-time * (empty ? 0.6f : 2.5f)));
        pose.mulPose(Axis.XP.rotationDegrees(8f));
        pose.scale(0.8f, 0.8f, 0.8f);
        ArcaneModels.render(pose, buffers, ArcaneModels.RUNE_RING, 0.95f * k, 0.8f * k, 0.45f * k, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
