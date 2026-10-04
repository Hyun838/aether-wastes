package com.aetherwastes.base;

import com.aetherwastes.block.WardPylonBlock;
import com.aetherwastes.ether.AnchorData;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Обереги-контуры: 4 и больше целых Рунных столбов в радиусе Якоря замыкают контур.
 * Внутри не появляются монстры, а забредшие выталкиваются (кроме волн Осады — те только ранятся).
 * Молния рядом повреждает столб; починка — Осколком Эфира.
 */
public final class Wards {
    private static final Map<ResourceKey<Level>, List<long[]>> WARDED = new HashMap<>();

    private Wards() {}

    public static boolean isWarded(ServerLevel level, BlockPos pos) {
        List<long[]> list = WARDED.get(level.dimension());
        if (list == null) return false;
        for (long[] w : list) {
            if (BlockPos.of(w[0]).distSqr(pos) <= (double) w[1] * w[1]) return true;
        }
        return false;
    }

    /** Каждые 2 секунды для каждого измерения. */
    public static void tick(ServerLevel level) {
        List<long[]> warded = new ArrayList<>();
        AnchorData data = AnchorData.get(level);
        for (Map.Entry<Long, Integer> a : data.all().entrySet()) {
            BlockPos anchor = BlockPos.of(a.getKey());
            int r = EtherField.anchorRadius(a.getValue());
            int pylons = 0;
            for (Map.Entry<Long, String> d : data.devices().entrySet()) {
                if (!Devices.WARD.equals(d.getValue())) continue;
                BlockPos pp = BlockPos.of(d.getKey());
                if (pp.distSqr(anchor) > (double) r * r || !level.isLoaded(pp)) continue;
                BlockState st = level.getBlockState(pp);
                if (st.is(ModBlocks.WARD_PYLON.get()) && !st.getValue(WardPylonBlock.DAMAGED)) pylons++;
            }
            if (pylons >= 4 && Devices.powered(level, anchor)) warded.add(new long[]{a.getKey(), r});
        }
        WARDED.put(level.dimension(), warded);

        for (long[] w : warded) {
            BlockPos c = BlockPos.of(w[0]);
            double r = w[1];
            if (!level.isLoaded(c)) continue;
            for (Monster m : level.getEntitiesOfClass(Monster.class, new AABB(c).inflate(r))) {
                if (m.position().distanceTo(Vec3.atCenterOf(c)) > r) continue;
                if (!m.getTags().contains("aw_siege")) {
                    Vec3 out = m.position().subtract(Vec3.atCenterOf(c)).normalize().scale(0.8);
                    m.push(out.x, 0.2, out.z);
                    m.hurtMarked = true;
                }
                m.hurt(level.damageSources().magic(), 1f);
                level.sendParticles(ParticleTypes.ENCHANTED_HIT, m.getX(), m.getY() + 1, m.getZ(), 4, 0.2, 0.3, 0.2, 0);
            }
        }
    }

    public static void onLightning(ServerLevel level, BlockPos strike) {
        for (BlockPos pos : BlockPos.betweenClosed(strike.offset(-6, -4, -6), strike.offset(6, 4, 6))) {
            BlockState st = level.getBlockState(pos);
            if (st.is(ModBlocks.WARD_PYLON.get()) && !st.getValue(WardPylonBlock.DAMAGED)) {
                level.setBlockAndUpdate(pos, st.setValue(WardPylonBlock.DAMAGED, true));
            }
        }
    }
}
