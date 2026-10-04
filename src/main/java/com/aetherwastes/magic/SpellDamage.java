package com.aetherwastes.magic;

import com.aetherwastes.entity.Glassman;
import com.aetherwastes.progression.Mastery;
import com.aetherwastes.progression.School;
import com.aetherwastes.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Урон от заклинаний: отражение Стеклянниками, уязвимость к Каменному Хору, Эссенции душ. */
public final class SpellDamage {
    private SpellDamage() {}

    public static boolean hurt(LivingEntity target, ServerPlayer caster, float amount, School school, Glyph form) {
        if (target instanceof Glassman glass) {
            if (form == Glyph.PROJECTILE || form == Glyph.WAVE) {
                caster.hurt(glass.damageSources().thorns(glass), amount * 0.5f);
                caster.serverLevel().sendParticles(ParticleTypes.END_ROD, glass.getX(), glass.getY() + 1.2, glass.getZ(),
                        12, 0.3, 0.4, 0.3, 0.05);
                return false;
            }
            if (school == School.STONE) amount *= 3f;
        }
        boolean hit = target.hurt(caster.damageSources().indirectMagic(caster, caster), amount);
        if (hit && !target.isAlive() && school == School.ASH) {
            target.spawnAtLocation(new ItemStack(ModItems.SOUL_ESSENCE.get()));
            Mastery.add(caster, School.ASH, 4);
        }
        return hit;
    }
}
