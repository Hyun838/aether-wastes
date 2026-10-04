package com.aetherwastes.survival;

import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.network.Sync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Травмы: перелом (падение), кровотечение (удары монстров), сотрясение (взрыв), ожог (огонь).
 * Лечатся шиной, бинтом, мазью или школой Течения; со временем проходят сами.
 */
public final class Traumas {
    public static final String FRACTURE = "fracture";
    public static final String BLEEDING = "bleeding";
    public static final String BURN = "burn";

    private static final long FRACTURE_HEAL = 24000L;
    private static final long BLEED_STOP = 2400L;
    private static final long BURN_HEAL = 12000L;

    private Traumas() {}

    public static void onDamaged(ServerPlayer p, DamageSource source, float amount) {
        if (!AetherConfig.TRAUMAS.get() || p.isCreative() || amount <= 0f) return;
        var rng = p.getRandom();
        if (source.is(DamageTypeTags.IS_FALL) && amount >= 5f && rng.nextFloat() < 0.6f) {
            give(p, FRACTURE);
        } else if (source.is(DamageTypeTags.IS_EXPLOSION) && amount >= 4f) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            Msg.bar(p, ChatFormatting.RED, "trauma.aetherwastes.concussion");
        } else if (source.is(DamageTypeTags.IS_FIRE) && amount >= 2f && rng.nextFloat() < 0.25f) {
            give(p, BURN);
        } else if (source.getEntity() instanceof Enemy && !source.is(DamageTypeTags.IS_PROJECTILE)
                && amount >= 4f && rng.nextFloat() < 0.25f) {
            give(p, BLEEDING);
        } else if (source.is(DamageTypeTags.IS_PROJECTILE) && amount >= 5f && rng.nextFloat() < 0.2f) {
            give(p, BLEEDING);
        }
    }

    public static void give(ServerPlayer p, String trauma) {
        PlayerData d = Data.get(p);
        if (d.traumas.containsKey(trauma)) return;
        d.traumas.put(trauma, p.level().getGameTime());
        Msg.bar(p, ChatFormatting.RED, "trauma.aetherwastes." + trauma + ".got");
        Sync.journal(p);
    }

    public static boolean cure(ServerPlayer p, String trauma) {
        PlayerData d = Data.get(p);
        if (d.traumas.remove(trauma) == null) return false;
        Msg.bar(p, ChatFormatting.GREEN, "trauma.aetherwastes." + trauma + ".cured");
        Sync.journal(p);
        return true;
    }

    /** Раз в секунду. */
    public static void tick(ServerPlayer p, PlayerData d) {
        if (d.traumas.isEmpty()) return;
        long now = p.level().getGameTime();
        if (d.traumas.containsKey(FRACTURE)) {
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
            if (now - d.traumas.get(FRACTURE) > FRACTURE_HEAL) cure(p, FRACTURE);
        }
        if (d.traumas.containsKey(BLEEDING)) {
            if (now % 60 < 20) p.hurt(p.damageSources().generic(), 0.5f);
            if (now - d.traumas.get(BLEEDING) > BLEED_STOP && p.getRandom().nextFloat() < 0.05f) cure(p, BLEEDING);
        }
        if (d.traumas.containsKey(BURN)) {
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
            if (now - d.traumas.get(BURN) > BURN_HEAL) cure(p, BURN);
        }
    }

    public static Component list(PlayerData d) {
        if (d.traumas.isEmpty()) return Component.translatable("trauma.aetherwastes.none");
        var out = Component.empty();
        boolean first = true;
        for (String t : d.traumas.keySet()) {
            if (!first) out.append(", ");
            out.append(Component.translatable("trauma.aetherwastes." + t));
            first = false;
        }
        return out;
    }
}
