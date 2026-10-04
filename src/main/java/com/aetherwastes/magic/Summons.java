package com.aetherwastes.magic;

import com.aetherwastes.core.Msg;
import com.aetherwastes.core.TempEntities;
import com.aetherwastes.progression.School;
import com.aetherwastes.threats.Scar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Форма «Призыв»: временный дух-союзник школы. У Пепельного Завета это Эхо — оно оставляет Порчу. */
public final class Summons {
    private Summons() {}

    public static void spawn(ServerPlayer p, SpellData spell, float power) {
        List<School> schools = spell.schools();
        if (schools.isEmpty()) return;
        String name = schools.size() > 1
                ? "hybrid.aetherwastes." + School.hybrid(schools.get(0), schools.get(1))
                : schools.get(0).key();
        Component title = Component.translatable("entity.aetherwastes.spirit", Component.translatable(name));
        int life = Math.round(20 * 60 * power);
        spawnAlly(p, title, life, schools.toArray(new School[0]));
        if (spell.has(School.ASH)) Scar.seedNear(p.serverLevel(), p.blockPosition(), 6);
    }

    /** Дух-волк, привязанный к игроку. Также используется Эхом Немезиды. */
    public static Wolf spawnAlly(ServerPlayer p, Component name, int lifeTicks, School... schools) {
        ServerLevel level = p.serverLevel();
        Wolf wolf = EntityType.WOLF.create(level);
        if (wolf == null) return null;
        Vec3 at = p.position().add(p.getLookAngle().multiply(1.5, 0, 1.5));
        wolf.moveTo(at.x, p.getY(), at.z, p.getYRot(), 0);
        wolf.tame(p);
        wolf.setCustomName(name.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        wolf.setCustomNameVisible(true);
        int inf = MobEffectInstance.INFINITE_DURATION;
        wolf.addEffect(new MobEffectInstance(MobEffects.GLOWING, inf, 0, false, false));
        for (School s : schools) {
            switch (s) {
                case FORGE -> {
                    wolf.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, inf, 0));
                    wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, inf, 1));
                }
                case FLOW -> wolf.addEffect(new MobEffectInstance(MobEffects.REGENERATION, inf, 1));
                case ROOT -> wolf.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, inf, 2));
                case SILENCE -> wolf.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, inf, 0));
                case STONE -> wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, inf, 1));
                case ASH -> wolf.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, inf, 2));
                case STAR -> wolf.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, inf, 2));
            }
        }
        TempEntities.mark(wolf, lifeTicks);
        level.addFreshEntity(wolf);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.x, p.getY() + 0.5, at.z, 30, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, p.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1f, 1.2f);
        Msg.bar(p, ChatFormatting.LIGHT_PURPLE, "message.aetherwastes.spell.summoned", lifeTicks / 20);
        return wolf;
    }
}
