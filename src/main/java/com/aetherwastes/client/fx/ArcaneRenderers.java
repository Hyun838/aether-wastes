package com.aetherwastes.client.fx;

import com.aetherwastes.block.GuardianSealBlock;
import com.aetherwastes.block.GuardianSealBlockEntity;
import com.aetherwastes.block.PurifyingObeliskBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Анимированные OBJ-объекты над блоками: кристалл Очищающего обелиска и сфера Печати стража.
 *
 * <p>Оптимизация: дальность отрисовки ограничена ({@link #VIEW}), кольца рун — деталь ближнего
 * плана и выключаются дальше {@link #DETAIL} блоков (LOD). Анимация считается от игрового
 * времени — у блок-сущностей нет тика на клиенте и нет состояния, которое надо хранить.
 */
public final class ArcaneRenderers {
    static final int VIEW = 64;
    static final double DETAIL = 24.0;

    private ArcaneRenderers() {}

    static boolean nearCamera(BlockEntity be, double dist) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return be.getBlockPos().getCenter().distanceToSqr(cam) < dist * dist;
    }

    static float time(BlockEntity be, float pt) {
        return be.getLevel() == null ? 0 : (be.getLevel().getGameTime() % 24000L) + pt;
    }

    /** Кристалл, парящий над обелиском, и кольцо рун вокруг него. */
    public static class Obelisk implements BlockEntityRenderer<PurifyingObeliskBlock.Entity> {
        public Obelisk(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(PurifyingObeliskBlock.Entity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            float t = time(be, pt);
            long seed = be.getBlockPos().asLong();
            float bob = 0.07f * (float) Math.sin(t / 14.0);
            float glow = ArcaneModels.pulse(t, seed, 0.08f);
            float y = 2.35f + bob;

            ArcaneModels.blobShadow(pose, buffers, 31.2f / 16f, 0.32f - bob, 0.5f);

            pose.pushPose();
            pose.translate(0.5, y, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(t * 1.5f));
            float s = 1.25f + 0.04f * glow;
            pose.scale(s, s, s);
            ArcaneModels.render(pose, buffers, ArcaneModels.CRYSTAL, 0.62f + 0.2f * glow, 0.95f, 1f, LightTexture.FULL_BRIGHT);
            pose.popPose();

            if (nearCamera(be, DETAIL)) {
                pose.pushPose();
                pose.translate(0.5, y - 0.05, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(-t * 2.2f));
                pose.mulPose(Axis.XP.rotationDegrees(12f + 6f * (float) Math.sin(t / 30.0)));
                pose.scale(0.75f, 0.75f, 0.75f);
                ArcaneModels.render(pose, buffers, ArcaneModels.RUNE_RING, 0.55f, 0.9f + 0.1f * glow, 1f, LightTexture.FULL_BRIGHT);
                pose.popPose();
            }
        }

        @Override
        public int getViewDistance() {
            return VIEW;
        }

        @Override
        public net.minecraft.world.phys.AABB getRenderBoundingBox(PurifyingObeliskBlock.Entity be) {
            return new net.minecraft.world.phys.AABB(be.getBlockPos()).expandTowards(0, 2.2, 0).inflate(0.6);
        }
    }

    /** Сфера Печати стража: висит над печатью, пока страж спит; цвет — по типу подземелья. */
    public static class Seal implements BlockEntityRenderer<GuardianSealBlockEntity> {
        private static final float[][] TINT = {{0.55f, 0.85f, 1f}, {0.78f, 0.55f, 1f}, {1f, 0.58f, 0.28f}};

        public Seal(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public void render(GuardianSealBlockEntity be, float pt, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            BlockState state = be.getBlockState();
            if (!state.hasProperty(GuardianSealBlock.ACTIVE) || !state.getValue(GuardianSealBlock.ACTIVE)) return;
            float[] c = TINT[Math.min(TINT.length - 1, state.getValue(GuardianSealBlock.KIND))];
            float t = time(be, pt);
            float glow = ArcaneModels.pulse(t, be.getBlockPos().asLong(), 0.11f);
            float y = 2.0f + 0.1f * (float) Math.sin(t / 18.0);

            ArcaneModels.blobShadow(pose, buffers, 20.2f / 16f, 0.55f, 0.55f);

            pose.pushPose();
            pose.translate(0.5, y, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(t * 0.8f));
            pose.mulPose(Axis.ZP.rotationDegrees(t * 0.5f));
            float s = 1.2f + 0.08f * glow;
            pose.scale(s, s, s);
            ArcaneModels.render(pose, buffers, ArcaneModels.SEAL_ORB, c[0], c[1], c[2], LightTexture.FULL_BRIGHT);
            pose.popPose();

            if (nearCamera(be, DETAIL)) {
                for (int i = 0; i < 2; i++) {
                    pose.pushPose();
                    pose.translate(0.5, y, 0.5);
                    pose.mulPose(Axis.YP.rotationDegrees(t * (i == 0 ? 2.4f : -1.7f) + i * 90f));
                    pose.mulPose(Axis.XP.rotationDegrees(i == 0 ? 70f : -35f));
                    float rs = 1.05f + 0.25f * i;
                    pose.scale(rs, rs, rs);
                    ArcaneModels.render(pose, buffers, ArcaneModels.RUNE_RING, c[0], c[1], c[2], LightTexture.FULL_BRIGHT);
                    pose.popPose();
                }
            }
        }

        @Override
        public int getViewDistance() {
            return VIEW;
        }

        @Override
        public net.minecraft.world.phys.AABB getRenderBoundingBox(GuardianSealBlockEntity be) {
            return new net.minecraft.world.phys.AABB(be.getBlockPos()).expandTowards(0, 2.5, 0).inflate(1.0);
        }
    }
}
