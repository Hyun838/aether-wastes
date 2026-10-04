package com.aetherwastes.survival;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Dims;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.craft.Engraving;
import com.aetherwastes.craft.Traits;
import com.aetherwastes.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Температура тела 0–100 (норма 50). Зависит от биома, времени суток, высоты, воды,
 * дождя, измерения, огня рядом, одежды и Эфирного стекла под солнцем.
 */
public final class Temperature {
    private Temperature() {}

    public static void tick(ServerPlayer p, PlayerData d) {
        if (!AetherConfig.TEMPERATURE.get()) {
            d.bodyTemp = 50f;
            return;
        }
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        boolean sky = level.canSeeSky(pos.above());

        float base = level.getBiome(pos).value().getBaseTemperature();
        float env = 50f + (base - 0.8f) * 25f;
        if (level.dimension() == Level.OVERWORLD || Dims.isUnderside(level)) {
            if (!level.isDay() && sky) env -= 8f;
            if (pos.getY() > 120) env -= (pos.getY() - 120) * 0.1f;
            if (!sky && pos.getY() < 50) env = Mth.lerp(0.7f, env, 50f);
        }
        if (level.dimension() == Level.NETHER) env += 35f;
        if (level.dimension() == Level.END) env -= 15f;
        if (p.isInWater()) env -= 15f;
        else if (level.isRainingAt(pos.above())) env -= 6f;

        // Огонь и Эфирное стекло рядом.
        int heat = 0;
        boolean etherGlass = false;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -3; x <= 3; x++)
            for (int y = -2; y <= 2; y++)
                for (int z = -3; z <= 3; z++) {
                    m.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    BlockState st = level.getBlockState(m);
                    if (st.is(Blocks.LAVA) || st.is(Blocks.FIRE) || st.is(Blocks.MAGMA_BLOCK) || st.is(ModBlocks.FORGE_HEARTH.get())) {
                        heat += 8;
                    } else if ((st.getBlock() instanceof CampfireBlock || st.getBlock() instanceof AbstractFurnaceBlock)
                            && st.hasProperty(BlockStateProperties.LIT) && st.getValue(BlockStateProperties.LIT)) {
                        heat += 10;
                    } else if (st.is(ModBlocks.ETHER_GLASS.get())) {
                        etherGlass = true;
                    }
                }
        env += Math.min(25, heat);
        if (etherGlass && level.isDay() && sky) {
            env += 15f;
            if (p.getRandom().nextFloat() < 0.05f) {
                p.igniteForSeconds(2f);
                Msg.bar(p, ChatFormatting.GOLD, "temperature.aetherwastes.glass_focus");
            }
        }
        if (p.isOnFire()) env += 20f;

        // Одежда.
        int insulation = 0;
        int heavy = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack s = p.getItemBySlot(slot);
            if (s.getItem() instanceof ArmorItem armor) {
                if (armor.getMaterial().value() == ArmorMaterials.LEATHER.value()) insulation += 5;
                else heavy++;
            }
            insulation += 6 * Traits.has(s, Traits.WARM);
        }
        if (level.getGameTime() < d.warmUntil) insulation += 20;
        if (env < 50f) env = Math.min(50f, env + insulation);
        if (env > 60f) env += heavy * 2f;
        if (Engraving.armorCount(p, "flow") > 0 && env > 55f) env -= 5f * Engraving.armorCount(p, "flow");
        if (p.hasEffect(MobEffects.FIRE_RESISTANCE) && env > 50f) env = Mth.lerp(0.6f, env, 50f);

        env = Mth.clamp(env, 0f, 100f);
        float diff = env - d.bodyTemp;
        d.bodyTemp += Mth.clamp(diff, -1.5f, 1.5f);

        if (p.isCreative()) return;
        long t = level.getGameTime();
        if (d.bodyTemp < 25f) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
            if (d.bodyTemp < 15f) {
                p.setTicksFrozen(Math.max(p.getTicksFrozen(), 140));
                if (t % 60 < 20) p.hurt(p.damageSources().freeze(), 1f);
                Msg.bar(p, ChatFormatting.AQUA, "temperature.aetherwastes.freezing");
            }
        } else if (d.bodyTemp > 75f) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
            p.getFoodData().addExhaustion(0.2f);
            if (d.bodyTemp > 88f) {
                if (t % 60 < 20) p.hurt(p.damageSources().onFire(), 1f);
                Msg.bar(p, ChatFormatting.GOLD, "temperature.aetherwastes.heatstroke");
            }
        }
    }
}
