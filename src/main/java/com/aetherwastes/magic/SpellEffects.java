package com.aetherwastes.magic;

import com.aetherwastes.block.RuneTrapBlockEntity;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.core.Scheduler;
import com.aetherwastes.progression.School;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.survival.Traumas;
import com.aetherwastes.threats.Pulse;
import com.aetherwastes.threats.Scar;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Что делают заклинания: цели по Форме, эффекты семи школ и девяти гибридов. */
public final class SpellEffects {
    private SpellEffects() {}

    /** Контекст одного срабатывания заклинания. */
    public static final class Ctx {
        public final ServerPlayer p;
        public final ServerLevel level;
        public final SpellData spell;
        public final Glyph form;
        public final List<LivingEntity> targets;
        public final Vec3 point;
        public final BlockHitResult block;
        public final boolean entityHit;
        public final float power;
        public final double radius;
        public final int expand;

        Ctx(ServerPlayer p, SpellData spell, Glyph form, List<LivingEntity> targets, Vec3 point,
            BlockHitResult block, boolean entityHit, float power, double radius) {
            this.p = p;
            this.level = p.serverLevel();
            this.spell = spell;
            this.form = form;
            this.targets = targets;
            this.point = point;
            this.block = block;
            this.entityHit = entityHit;
            this.power = power;
            this.radius = radius;
            this.expand = spell.count(Glyph.EXPAND);
        }
    }

    // ===================== Цели =====================

    public static void execute(ServerPlayer p, SpellData spell, float power) {
        if (!p.isAlive() || p.isRemoved()) return;
        int expand = spell.count(Glyph.EXPAND);
        Glyph form = spell.form();
        ServerLevel level = p.serverLevel();

        switch (form) {
            case SUMMON -> {
                Summons.spawn(p, spell, power);
                return;
            }
            case RUNE -> {
                placeRune(p, spell, power, 8 + 4 * expand);
                return;
            }
            default -> {
            }
        }

        List<LivingEntity> targets = new ArrayList<>();
        Vec3 point = p.position();
        BlockHitResult block = null;
        boolean entityHit = false;
        double radius = 0;

        switch (form) {
            case SELF -> targets.add(p);
            case TOUCH, PROJECTILE -> {
                double range = form == Glyph.TOUCH ? 4.5 + expand : 24 + 8 * expand;
                Vec3 eye = p.getEyePosition();
                Vec3 end = eye.add(p.getLookAngle().scale(range));
                block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
                if (block.getType() != HitResult.Type.MISS) end = block.getLocation();
                LivingEntity hit = rayEntity(p, eye, end);
                if (hit != null) {
                    targets.add(hit);
                    entityHit = true;
                    point = hit.getBoundingBox().getCenter();
                } else {
                    point = end;
                }
                trail(level, eye.add(p.getLookAngle().scale(0.8)), point, particleFor(spell));
                if (block.getType() == HitResult.Type.MISS) block = null;
            }
            case ZONE -> {
                radius = 4 + 2.5 * expand;
                targets.addAll(around(p, p.position(), radius));
                ring(level, p.position(), radius, particleFor(spell));
            }
            case WAVE -> {
                radius = 8 + 3 * expand;
                Vec3 look = p.getLookAngle();
                for (LivingEntity e : around(p, p.position(), radius)) {
                    if (e == p) continue;
                    Vec3 dir = e.position().subtract(p.position()).normalize();
                    if (dir.dot(look) > 0.8) targets.add(e);
                }
                cone(level, p, radius, particleFor(spell));
                point = p.getEyePosition().add(look.scale(radius / 2));
            }
            default -> {
            }
        }

        // Цепь: перескакивает на ближайших врагов.
        int chain = spell.count(Glyph.CHAIN);
        if (chain > 0 && !targets.isEmpty() && form != Glyph.SELF && form != Glyph.ZONE) {
            LivingEntity from = targets.get(0);
            List<LivingEntity> extra = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                    from.getBoundingBox().inflate(6), e -> e.isAlive() && e != p && !targets.contains(e) && e instanceof Enemy));
            extra.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
            for (int i = 0; i < Math.min(extra.size(), 2 * chain); i++) {
                LivingEntity e = extra.get(i);
                trail(level, from.getBoundingBox().getCenter(), e.getBoundingBox().getCenter(), ParticleTypes.ELECTRIC_SPARK);
                targets.add(e);
            }
        }

        applyAll(new Ctx(p, spell, form, targets, point, block, entityHit, power, radius));
    }

    /** Применить все Сути (и бонус гибрида). Используется и рунами-ловушками. */
    public static void applyAll(Ctx c) {
        List<School> schools = c.spell.schools();
        float factor = schools.size() > 1 ? 0.75f : 1f;
        for (School s : schools) apply(s, c, c.power * factor);
        if (schools.size() > 1) hybrid(School.hybrid(schools.get(0), schools.get(1)), c);
    }

    public static Ctx runeContext(ServerPlayer owner, SpellData spell, float power, LivingEntity victim) {
        List<LivingEntity> t = new ArrayList<>();
        t.add(victim);
        return new Ctx(owner, spell, Glyph.RUNE, t, victim.getBoundingBox().getCenter(), null, true, power, 0);
    }

    private static LivingEntity rayEntity(ServerPlayer p, Vec3 eye, Vec3 end) {
        AABB box = new AABB(eye, end).inflate(1.0);
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : p.serverLevel().getEntities(p, box, e -> e instanceof LivingEntity && e.isAlive() && e.isPickable())) {
            var hit = e.getBoundingBox().inflate(0.3).clip(eye, end);
            if (hit.isPresent()) {
                double d = eye.distanceToSqr(hit.get());
                if (d < bestDist) {
                    bestDist = d;
                    best = (LivingEntity) e;
                }
            }
        }
        return best;
    }

    private static List<LivingEntity> around(ServerPlayer p, Vec3 center, double radius) {
        List<LivingEntity> list = new ArrayList<>(p.serverLevel().getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(radius), e -> e.isAlive() && e.position().distanceTo(center) <= radius));
        list.sort(Comparator.comparingDouble(e -> e.distanceToSqr(center)));
        return list;
    }

    public static boolean isAlly(ServerPlayer p, LivingEntity t) {
        if (t == p || t instanceof Player) return true;
        return t instanceof OwnableEntity o && p.getUUID().equals(o.getOwnerUUID());
    }

    public static boolean canHarm(Ctx c, LivingEntity t) {
        if (isAlly(c.p, t)) return false;
        if (t instanceof Enemy) return true;
        return c.form == Glyph.TOUCH || c.form == Glyph.PROJECTILE || c.form == Glyph.RUNE;
    }

    // ===================== Школы =====================

    private static void apply(School s, Ctx c, float pw) {
        switch (s) {
            case FORGE -> forge(c, pw);
            case FLOW -> flow(c, pw);
            case ROOT -> root(c, pw);
            case SILENCE -> silence(c, pw);
            case STONE -> stone(c, pw);
            case ASH -> ash(c, pw);
            case STAR -> star(c, pw);
        }
    }

    private static void forge(Ctx c, float pw) {
        c.level.playSound(null, c.p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1f, 0.9f);
        if (c.form == Glyph.SELF) {
            c.p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Math.round(600 * pw), 0));
            c.p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, Math.round(200 * pw), pw > 1.4f ? 1 : 0));
            Data.get(c.p).warmUntil = c.level.getGameTime() + Math.round(1200 * pw);
            burst(c.level, c.p.position().add(0, 1, 0), ParticleTypes.FLAME, 30);
            if (c.p.getRandom().nextFloat() < 0.15f) {
                c.p.igniteForSeconds(2f);
                Msg.bar(c.p, ChatFormatting.GOLD, "message.aetherwastes.spell.overheat");
            }
            return;
        }
        for (LivingEntity t : c.targets) {
            if (!canHarm(c, t)) continue;
            SpellDamage.hurt(t, c.p, 4f * pw, School.FORGE, c.form);
            t.igniteForSeconds(3f * pw);
            burst(c.level, t.getBoundingBox().getCenter(), ParticleTypes.FLAME, 20);
        }
        if (c.form == Glyph.ZONE || c.form == Glyph.WAVE) {
            Scar.cleanse(c.level, c.p.blockPosition(), (int) Math.ceil(c.radius));
        } else if (!c.entityHit && c.block != null) {
            Scar.cleanse(c.level, c.block.getBlockPos(), 2);
        }
    }

    private static void flow(Ctx c, float pw) {
        c.level.playSound(null, c.p.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.8f, 1.2f);
        float healed = 0f;
        if (c.form == Glyph.SELF) {
            float amount = 6f * pw;
            c.p.heal(amount);
            c.p.clearFire();
            Traumas.cure(c.p, Traumas.BLEEDING);
            Traumas.cure(c.p, Traumas.BURN);
            if (pw >= 1.3f) Traumas.cure(c.p, Traumas.FRACTURE);
            PlayerData d = Data.get(c.p);
            if (d.bodyTemp > 50f) d.bodyTemp = Math.max(50f, d.bodyTemp - 15f);
            healed += amount;
            burst(c.level, c.p.getBoundingBox().getCenter(), ParticleTypes.SPLASH, 30);
        } else {
            for (LivingEntity t : c.targets) {
                if (isAlly(c.p, t) || !(t instanceof Enemy) && !canHarm(c, t)) {
                    float amount = (t == c.p ? 6f : 4f) * pw;
                    t.heal(amount);
                    t.clearFire();
                    healed += amount;
                    burst(c.level, t.getBoundingBox().getCenter(), ParticleTypes.HAPPY_VILLAGER, 10);
                } else if (c.form == Glyph.TOUCH) {
                    // Кровопуск: вытягивает жизнь врага.
                    if (SpellDamage.hurt(t, c.p, 3f * pw, School.FLOW, c.form)) c.p.heal(1.5f * pw);
                    burst(c.level, t.getBoundingBox().getCenter(), ParticleTypes.DAMAGE_INDICATOR, 8);
                } else {
                    // Водоворот: отбрасывает и замедляет.
                    Vec3 push = t.position().subtract(c.p.position()).normalize().scale(0.9 * pw);
                    t.push(push.x, 0.35, push.z);
                    t.hurtMarked = true;
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(100 * pw), 1));
                    t.clearFire();
                    burst(c.level, t.getBoundingBox().getCenter(), ParticleTypes.SPLASH, 30);
                }
            }
        }
        if (healed > 0f) c.p.getFoodData().addExhaustion(healed * 0.6f);
    }

    private static void root(Ctx c, float pw) {
        c.level.playSound(null, c.p.blockPosition(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 1f, 0.8f);
        if (c.form == Glyph.SELF) {
            c.p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.round(200 * pw), 1));
            burst(c.level, c.p.getBoundingBox().getCenter(), ParticleTypes.HAPPY_VILLAGER, 20);
            return;
        }
        for (LivingEntity t : c.targets) {
            if (isAlly(c.p, t)) {
                t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, Math.round(160 * pw), 0));
            } else if (canHarm(c, t)) {
                // Терновые путы.
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(60 * pw), 4));
                SpellDamage.hurt(t, c.p, 2f * pw, School.ROOT, c.form);
                burst(c.level, t.position(), ParticleTypes.COMPOSTER, 20);
            }
        }
        if (c.form == Glyph.ZONE) {
            growCrops(c.level, c.p.blockPosition(), (int) Math.ceil(c.radius), 12);
        } else if (c.form == Glyph.PROJECTILE && !c.entityHit && c.block != null) {
            livingWall(c);
        }
    }

    private static void silence(Ctx c, float pw) {
        c.level.playSound(null, c.p.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 0.6f, 0.5f);
        if (c.form == Glyph.SELF) {
            c.p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Math.round(400 * pw), 0));
            Pulse.addNoise(c.p, -30f);
            burst(c.level, c.p.getBoundingBox().getCenter(), ParticleTypes.SMOKE, 30);
            return;
        }
        for (LivingEntity t : c.targets) {
            if (isAlly(c.p, t)) {
                if (c.form == Glyph.ZONE) t.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Math.round(120 * pw), 0));
            } else if (canHarm(c, t)) {
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.round(80 * pw), 0));
                if (t instanceof Mob m) m.setTarget(null);
                burst(c.level, t.getBoundingBox().getCenter(), ParticleTypes.SQUID_INK, 15);
            }
            if (t instanceof ServerPlayer other && other != c.p && c.form == Glyph.TOUCH) {
                Data.get(other).silencedUntil = c.level.getGameTime() + Math.round(100 * pw);
            }
        }
    }

    private static void stone(Ctx c, float pw) {
        if (c.form == Glyph.SELF) {
            c.p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Math.round(400 * pw), 1));
            c.p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(400 * pw), 0));
            c.level.playSound(null, c.p.blockPosition(), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1f, 0.5f);
            if (c.expand > 0) echolocate(c.p, 10 + 3 * c.expand);
            return;
        }
        c.level.playSound(null, c.p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.6f, 1.3f);
        for (LivingEntity t : c.targets) {
            if (!canHarm(c, t)) continue;
            SpellDamage.hurt(t, c.p, 5f * pw, School.STONE, c.form);
            t.push(0, 0.6, 0);
            t.hurtMarked = true;
            c.level.sendParticles(ParticleTypes.SONIC_BOOM, t.getX(), t.getY() + 1, t.getZ(), 1, 0, 0, 0, 0);
        }
        if (c.form == Glyph.ZONE) {
            BlockState below = c.level.getBlockState(c.p.blockPosition().below());
            c.level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, below),
                    c.p.getX(), c.p.getY() + 0.1, c.p.getZ(), 60, c.radius / 2, 0.1, c.radius / 2, 0.1);
            echolocate(c.p, (int) Math.min(16, 8 + c.radius));
        }
    }

    private static void ash(Ctx c, float pw) {
        c.level.playSound(null, c.p.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1f, 0.7f);
        if (c.form == Glyph.SELF) {
            // Похоронный колокол.
            c.level.playSound(null, c.p.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.5f, 0.5f);
            for (LivingEntity e : around(c.p, c.p.position(), 24 * Math.min(2f, pw))) {
                if (e instanceof Enemy) e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0));
                if (e.getType().is(EntityTypeTags.UNDEAD) && e.distanceTo(c.p) <= 8 * pw) {
                    SpellDamage.hurt(e, c.p, 6f * pw, School.ASH, c.form);
                }
            }
        } else {
            for (LivingEntity t : c.targets) {
                if (!canHarm(c, t)) continue;
                t.addEffect(new MobEffectInstance(MobEffects.WITHER, Math.round(100 * pw), pw > 1.4f ? 1 : 0));
                SpellDamage.hurt(t, c.p, 3f * pw, School.ASH, c.form);
                burst(c.level, t.getBoundingBox().getCenter(), ParticleTypes.SOUL, 15);
            }
        }
        if (c.level.random.nextFloat() < 0.25f) Scar.seedNear(c.level, c.p.blockPosition(), 8);
    }

    private static void star(Ctx c, float pw) {
        c.level.playSound(null, c.p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.6f);
        PlayerData d = Data.get(c.p);
        switch (c.form) {
            case SELF -> {
                if (c.p.isShiftKeyDown()) {
                    CompoundTag mark = new CompoundTag();
                    mark.putString("dim", c.level.dimension().location().toString());
                    mark.putDouble("x", c.p.getX());
                    mark.putDouble("y", c.p.getY());
                    mark.putDouble("z", c.p.getZ());
                    d.returnMark = mark;
                    Msg.bar(c.p, ChatFormatting.AQUA, "message.aetherwastes.spell.mark_set");
                } else if (d.returnMark.contains("dim")
                        && d.returnMark.getString("dim").equals(c.level.dimension().location().toString())) {
                    burst(c.level, c.p.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 40);
                    c.p.teleportTo(c.level, d.returnMark.getDouble("x"), d.returnMark.getDouble("y"),
                            d.returnMark.getDouble("z"), c.p.getYRot(), c.p.getXRot());
                    c.level.playSound(null, c.p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.2f);
                } else {
                    Msg.bar(c.p, ChatFormatting.GRAY, "message.aetherwastes.spell.no_mark");
                }
            }
            case PROJECTILE -> {
                if (c.entityHit) {
                    starStrike(c, pw);
                } else if (c.block != null) {
                    BlockPos land = c.block.getBlockPos().relative(c.block.getDirection());
                    if (c.level.getBlockState(land).getCollisionShape(c.level, land).isEmpty()
                            && c.level.getBlockState(land.above()).getCollisionShape(c.level, land.above()).isEmpty()) {
                        burst(c.level, c.p.position().add(0, 1, 0), ParticleTypes.REVERSE_PORTAL, 30);
                        c.p.teleportTo(c.level, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, c.p.getYRot(), c.p.getXRot());
                        c.p.fallDistance = 0;
                        c.level.playSound(null, land, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.5f);
                    }
                }
            }
            case TOUCH, RUNE -> starStrike(c, pw);
            default -> {
                for (LivingEntity t : c.targets) {
                    if (isAlly(c.p, t)) {
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, Math.round(120 * pw), 1));
                    } else if (canHarm(c, t)) {
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.round(120 * pw), 3));
                        t.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, Math.round(120 * pw), 1));
                        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.round(120 * pw), 0));
                        burst(c.level, t.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 10);
                    }
                }
            }
        }
    }

    private static void starStrike(Ctx c, float pw) {
        for (LivingEntity t : c.targets) {
            if (!canHarm(c, t)) continue;
            SpellDamage.hurt(t, c.p, 5f * pw, School.STAR, c.form);
            t.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
            burst(c.level, t.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 25);
        }
    }

    // ===================== Гибриды =====================

    private static void hybrid(String id, Ctx c) {
        float pw = c.power;
        switch (id) {
            case "steam" -> {
                for (LivingEntity t : c.targets) if (canHarm(c, t)) {
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    SpellDamage.hurt(t, c.p, 2f * pw, School.FLOW, c.form);
                }
                burst(c.level, c.point, ParticleTypes.CLOUD, 60);
            }
            case "rot" -> {
                for (LivingEntity t : c.targets) if (canHarm(c, t)) {
                    t.addEffect(new MobEffectInstance(MobEffects.POISON, Math.round(120 * pw), 1));
                }
                Scar.cleanse(c.level, BlockPos.containing(c.point), 6);
            }
            case "gravity" -> {
                for (LivingEntity t : c.targets) if (canHarm(c, t)) {
                    Vec3 pull = c.point.subtract(t.position()).scale(0.25);
                    t.push(pull.x, 0.8, pull.z);
                    t.hurtMarked = true;
                }
                Scheduler.after(12, () -> {
                    for (LivingEntity t : c.targets) if (t.isAlive() && canHarm(c, t)) {
                        t.push(0, -1.5, 0);
                        t.hurtMarked = true;
                        SpellDamage.hurt(t, c.p, 3f * pw, School.STONE, c.form);
                    }
                });
            }
            case "magma" -> {
                for (LivingEntity t : c.targets) if (canHarm(c, t)) {
                    t.igniteForSeconds(4f);
                    t.push(0, 0.5, 0);
                    t.hurtMarked = true;
                }
                burst(c.level, c.point, ParticleTypes.LAVA, 20);
            }
            case "bloom" -> {
                for (LivingEntity t : c.targets) if (isAlly(c.p, t)) {
                    t.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 0));
                }
                growCrops(c.level, BlockPos.containing(c.point), 4, 8);
            }
            case "dread" -> {
                for (LivingEntity t : c.targets) if (canHarm(c, t)) {
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                    if (t instanceof Mob m) m.setTarget(null);
                    Vec3 away = t.position().subtract(c.p.position()).normalize().scale(1.2);
                    t.push(away.x, 0.2, away.z);
                    t.hurtMarked = true;
                }
            }
            case "flare" -> {
                for (LivingEntity t : c.targets) if (canHarm(c, t)) {
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                }
                burst(c.level, c.point, ParticleTypes.FLASH, 1);
            }
            case "mist" -> {
                for (LivingEntity t : around(c.p, c.p.position(), Math.max(4, c.radius))) {
                    if (isAlly(c.p, t)) t.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 160, 0));
                    else if (t instanceof Mob m) m.setTarget(null);
                }
                burst(c.level, c.p.position(), ParticleTypes.CLOUD, 80);
            }
            case "bramble" -> {
                for (LivingEntity t : c.targets) if (canHarm(c, t)) {
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 5));
                    SpellDamage.hurt(t, c.p, 2f * pw, School.ROOT, c.form);
                }
            }
            default -> {
            }
        }
    }

    // ===================== Помощники =====================

    private static void placeRune(ServerPlayer p, SpellData spell, float power, double range) {
        ServerLevel level = p.serverLevel();
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(p.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() == HitResult.Type.MISS) {
            Msg.bar(p, ChatFormatting.GRAY, "message.aetherwastes.spell.rune_no_surface");
            return;
        }
        BlockPos pos = hit.getDirection() == Direction.UP ? hit.getBlockPos().above() : hit.getBlockPos().relative(hit.getDirection());
        if (!level.getBlockState(pos).canBeReplaced() || !level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
            Msg.bar(p, ChatFormatting.GRAY, "message.aetherwastes.spell.rune_no_surface");
            return;
        }
        level.setBlockAndUpdate(pos, ModBlocks.RUNE_TRAP.get().defaultBlockState());
        if (level.getBlockEntity(pos) instanceof RuneTrapBlockEntity be) {
            be.arm(p.getUUID(), spell, power);
        }
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1.4f);
        Msg.bar(p, ChatFormatting.LIGHT_PURPLE, "message.aetherwastes.spell.rune_placed");
    }

    private static void livingWall(Ctx c) {
        Direction face = c.block.getDirection();
        BlockPos base = c.block.getBlockPos().relative(face);
        Direction side = c.p.getDirection().getClockWise();
        int placed = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                BlockPos pos = base.relative(side, dx).above(dy);
                if (c.level.getBlockState(pos).canBeReplaced()) {
                    c.level.setBlockAndUpdate(pos, ModBlocks.LIVING_WALL.get().defaultBlockState());
                    placed++;
                }
            }
        }
        if (placed > 0) c.level.playSound(null, base, SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.PLAYERS, 1f, 0.8f);
    }

    private static void growCrops(ServerLevel level, BlockPos center, int r, int max) {
        int done = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -2, -r), center.offset(r, 2, r))) {
            if (done >= max) break;
            BlockState st = level.getBlockState(pos);
            if (st.is(BlockTags.CROPS) || st.is(BlockTags.SAPLINGS)) {
                if (BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos.immutable())) {
                    level.levelEvent(1505, pos, 0);
                    done++;
                }
            }
        }
    }

    /** Эхолокация Каменного Хора: сообщает о рудах вокруг. */
    private static void echolocate(ServerPlayer p, int r) {
        ServerLevel level = p.serverLevel();
        Map<String, Integer> counts = new HashMap<>();
        BlockPos nearest = null;
        String nearestName = null;
        double best = Double.MAX_VALUE;
        int shown = 0;
        BlockPos c = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-r, -r, -r), c.offset(r, r, r))) {
            BlockState st = level.getBlockState(pos);
            if (!st.is(Tags.Blocks.ORES)) continue;
            String name = st.getBlock().getDescriptionId();
            counts.merge(name, 1, Integer::sum);
            double d = pos.distSqr(c);
            if (d < best) {
                best = d;
                nearest = pos.immutable();
                nearestName = name;
            }
            if (shown < 40) {
                level.sendParticles(p, ParticleTypes.END_ROD, true, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        1, 0, 0, 0, 0);
                shown++;
            }
        }
        if (counts.isEmpty()) {
            Msg.chat(p, ChatFormatting.GRAY, "message.aetherwastes.spell.echo_none", r);
            return;
        }
        Msg.chat(p, ChatFormatting.GRAY, "message.aetherwastes.spell.echo_header", r);
        counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(5).forEach(e ->
                p.displayClientMessage(net.minecraft.network.chat.Component.literal("  ")
                        .append(net.minecraft.network.chat.Component.translatable(e.getKey()))
                        .append(": " + e.getValue()).withStyle(ChatFormatting.GRAY), false));
        if (nearest != null) {
            int dy = nearest.getY() - c.getY();
            Msg.chat(p, ChatFormatting.AQUA, "message.aetherwastes.spell.echo_nearest",
                    net.minecraft.network.chat.Component.translatable(nearestName),
                    Math.round(Math.sqrt(best)), dy);
        }
    }

    public static ParticleOptions particleFor(SpellData spell) {
        List<School> s = spell.schools();
        if (s.isEmpty()) return ParticleTypes.WITCH;
        return switch (s.get(0)) {
            case FORGE -> ParticleTypes.FLAME;
            case FLOW -> ParticleTypes.SPLASH;
            case ROOT -> ParticleTypes.COMPOSTER;
            case SILENCE -> ParticleTypes.SMOKE;
            case STONE -> ParticleTypes.CRIT;
            case ASH -> ParticleTypes.SOUL;
            case STAR -> ParticleTypes.END_ROD;
        };
    }

    public static void burst(ServerLevel level, Vec3 at, ParticleOptions type, int count) {
        level.sendParticles(type, at.x, at.y, at.z, count, 0.35, 0.45, 0.35, 0.02);
    }

    public static void trail(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions type) {
        Vec3 d = to.subtract(from);
        int steps = Math.max(1, (int) (d.length() * 2));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.add(d.scale(i / (double) steps));
            level.sendParticles(type, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    private static void ring(ServerLevel level, Vec3 center, double radius, ParticleOptions type) {
        int n = (int) (radius * 8);
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            level.sendParticles(type, center.x + Math.cos(a) * radius, center.y + 0.2,
                    center.z + Math.sin(a) * radius, 1, 0, 0.05, 0, 0);
        }
    }

    private static void cone(ServerLevel level, ServerPlayer p, double range, ParticleOptions type) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        for (int i = 0; i < 40; i++) {
            Vec3 dir = look.add((level.random.nextDouble() - 0.5) * 0.7, (level.random.nextDouble() - 0.5) * 0.4,
                    (level.random.nextDouble() - 0.5) * 0.7).normalize();
            Vec3 at = eye.add(dir.scale(level.random.nextDouble() * range));
            level.sendParticles(type, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }
}
