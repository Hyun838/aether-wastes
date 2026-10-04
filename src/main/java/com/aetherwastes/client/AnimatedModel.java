package com.aetherwastes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;

import java.util.NoSuchElementException;

/**
 * Универсальная модель мобов Пустошей. Части ищутся по именам (head, body, right_arm, left_arm,
 * right_leg, left_leg, skirt, halo, orbit, wing_left, wing_right, staff), анимация задаётся стилем.
 */
public class AnimatedModel<T extends Mob> extends HierarchicalModel<T> implements ArmedModel {
    public enum Style {BIPED, HEAVY, FLOAT, CASTER}

    private final ModelPart root;
    private final Style style;
    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg, skirt, halo, orbit, wingLeft, wingRight;

    public AnimatedModel(ModelPart root, Style style) {
        this.root = root;
        this.style = style;
        this.head = find(root, "head");
        this.body = find(root, "body");
        this.rightArm = find(root, "right_arm");
        this.leftArm = find(root, "left_arm");
        this.rightLeg = find(root, "right_leg");
        this.leftLeg = find(root, "left_leg");
        this.skirt = find(root, "skirt");
        this.orbit = find(root, "orbit");
        this.halo = head == null ? null : find(head, "halo");
        this.wingLeft = body == null ? null : find(body, "wing_left");
        this.wingRight = body == null ? null : find(body, "wing_right");
    }

    private static ModelPart find(ModelPart parent, String name) {
        try {
            return parent.getChild(name);
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public ModelPart root() {
        return root;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float age, float netHeadYaw, float headPitch) {
        root.getAllParts().forEach(ModelPart::resetPose);
        boolean aggressive = entity.isAggressive();
        float walk = limbSwing * 0.6662f;

        if (head != null) {
            head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
            head.xRot = headPitch * Mth.DEG_TO_RAD;
        }

        float legAmp = style == Style.HEAVY ? 1.0f : 1.4f;
        if (rightLeg != null) rightLeg.xRot = Mth.cos(walk) * legAmp * limbSwingAmount;
        if (leftLeg != null) leftLeg.xRot = Mth.cos(walk + Mth.PI) * legAmp * limbSwingAmount;

        switch (style) {
            case BIPED, HEAVY -> {
                if (aggressive) {
                    reach(age, 0.08f);
                } else {
                    if (rightArm != null) rightArm.xRot = Mth.cos(walk + Mth.PI) * limbSwingAmount;
                    if (leftArm != null) leftArm.xRot = Mth.cos(walk) * limbSwingAmount;
                }
                if (style == Style.HEAVY && body != null) {
                    body.zRot = Mth.cos(walk) * 0.06f * limbSwingAmount;
                    if (head != null) head.zRot = body.zRot;
                }
                idleArms(age);
            }
            case FLOAT -> {
                root.y += Mth.sin(age * 0.09f) * 1.5f - 1.0f;
                if (aggressive) {
                    reach(age, 0.12f);
                } else {
                    if (rightArm != null) rightArm.xRot = -0.25f + Mth.sin(age * 0.07f) * 0.12f;
                    if (leftArm != null) leftArm.xRot = -0.25f + Mth.sin(age * 0.07f + 1.5f) * 0.12f;
                }
                if (skirt != null) {
                    skirt.xRot = 0.15f + limbSwingAmount * 0.5f + Mth.sin(age * 0.12f) * 0.06f;
                    skirt.zRot = Mth.sin(age * 0.09f) * 0.05f;
                }
            }
            case CASTER -> {
                root.y += Mth.sin(age * 0.05f) * 0.6f;
                if (rightArm != null) {
                    rightArm.xRot = -0.35f + Mth.cos(walk) * 0.2f * limbSwingAmount;
                    rightArm.zRot = 0.08f;
                }
                if (leftArm != null) {
                    if (aggressive) {
                        leftArm.xRot = -1.9f + Mth.sin(age * 0.3f) * 0.25f;
                        leftArm.zRot = -0.25f + Mth.cos(age * 0.3f) * 0.1f;
                    } else {
                        leftArm.xRot = Mth.cos(walk) * 0.5f * limbSwingAmount;
                        leftArm.zRot = -0.06f;
                    }
                }
                if (skirt != null) skirt.xRot = Mth.cos(walk) * 0.18f * limbSwingAmount + 0.04f;
            }
        }

        if (halo != null) {
            halo.yRot = age * 0.05f;
            halo.y += Mth.sin(age * 0.08f) * 0.6f;
        }
        if (orbit != null) orbit.yRot = age * 0.06f;
        if (wingLeft != null) wingLeft.yRot += Mth.sin(age * 0.1f) * 0.15f;
        if (wingRight != null) wingRight.yRot -= Mth.sin(age * 0.1f) * 0.15f;
    }

    private void reach(float age, float sway) {
        if (rightArm != null) {
            rightArm.xRot = -Mth.HALF_PI + Mth.sin(age * 0.067f) * sway;
            rightArm.zRot = Mth.cos(age * 0.09f) * 0.05f + 0.05f;
        }
        if (leftArm != null) {
            leftArm.xRot = -Mth.HALF_PI - Mth.sin(age * 0.067f) * sway;
            leftArm.zRot = -Mth.cos(age * 0.09f) * 0.05f - 0.05f;
        }
    }

    private void idleArms(float age) {
        if (rightArm != null) rightArm.zRot += Mth.cos(age * 0.09f) * 0.05f + 0.05f;
        if (leftArm != null) leftArm.zRot -= Mth.cos(age * 0.09f) * 0.05f + 0.05f;
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack pose) {
        root.translateAndRotate(pose);
        ModelPart part = arm == HumanoidArm.RIGHT ? rightArm : leftArm;
        if (part != null) part.translateAndRotate(pose);
    }
}
