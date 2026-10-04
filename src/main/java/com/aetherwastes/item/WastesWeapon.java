package com.aetherwastes.item;

import com.aetherwastes.core.Msg;
import com.aetherwastes.magic.SpellEffects;
import com.aetherwastes.magic.Summons;
import com.aetherwastes.progression.School;
import com.aetherwastes.survival.PlayerStats;
import com.aetherwastes.threats.Pulse;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Оружие Пустошей. У каждого — своё свойство при ударе, у реликвий Скитальцев —
 * ещё и способность по ПКМ (стоит Эфира и имеет перезарядку).
 */
public class WastesWeapon extends SwordItem {
    public enum Ability {
        ETHER_DRAIN(0, 0), SWEEP(0, 0), EMBER(0, 0), BACKSTAB(0, 0), NIGHTFALL(0, 0),
        FIREBOLT(6, 16), SLAM(18, 80), BLINK(12, 50), HEART(30, 600);

        public final int cost;
        public final int cooldown;

        Ability(int cost, int cooldown) {
            this.cost = cost;
            this.cooldown = cooldown;
        }

        public String key() {
            return "weapon.aetherwastes." + name().toLowerCase();
        }
    }

    private final Ability ability;

    public WastesWeapon(Tier tier, Ability ability, Properties properties) {
        super(tier, properties);
        this.ability = ability;
    }

    public Ability ability() {
        return ability;
    }

    /** Множитель урона от ситуации (кинжал в спину, меч ночью). Вызывается из CombatEvents. */
    public float damageMultiplier(Player attacker, LivingEntity target) {
        switch (ability) {
            case BACKSTAB -> {
                Vec3 look = target.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 toAttacker = attacker.position().subtract(target.position()).multiply(1, 0, 1).normalize();
                return look.dot(toAttacker) < -0.3 ? 2.0f : 1.0f;
            }
            case NIGHTFALL -> {
                return attacker.level().isDay() ? 1.0f : 1.5f;
            }
            default -> {
                return 1.0f;
            }
        }
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean r = super.hurtEnemy(stack, target, attacker);
        if (!(attacker instanceof ServerPlayer p) || !(p.level() instanceof ServerLevel level)) return r;
        switch (ability) {
            case ETHER_DRAIN -> {
                PlayerStats.setVessel(p, PlayerStats.vessel(p) + 4f);
                level.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
            }
            case SWEEP -> {
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3),
                        e -> e != p && e != target && e instanceof Enemy && e.isAlive())) {
                    e.hurt(p.damageSources().playerAttack(p), 4f);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                }
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 3, 1.2, 0.2, 1.2, 0);
            }
            case EMBER -> {
                target.igniteForSeconds(5f);
                level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY() + 1, target.getZ(), 6, 0.3, 0.3, 0.3, 0);
            }
            case BACKSTAB -> level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
            case NIGHTFALL -> {
                if (!level.isDay()) level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.4, 0.3, 0.05);
            }
            case FIREBOLT -> target.igniteForSeconds(3f);
            case SLAM -> {
                target.push(0, 0.5, 0);
                target.hurtMarked = true;
            }
            case BLINK -> target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
            case HEART -> p.heal(Math.max(1f, target.getMaxHealth() * 0.04f));
        }
        return r;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ability.cost == 0) return super.use(level, player, hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        ServerPlayer p = (ServerPlayer) player;
        if (!p.getAbilities().instabuild && PlayerStats.vessel(p) < ability.cost) {
            Msg.bar(p, ChatFormatting.RED, "message.aetherwastes.spell.no_ether", ability.cost, Math.round(PlayerStats.vessel(p)));
            return InteractionResultHolder.fail(stack);
        }
        ServerLevel server = (ServerLevel) level;
        boolean done = switch (ability) {
            case FIREBOLT -> firebolt(server, p);
            case SLAM -> slam(server, p);
            case BLINK -> blink(server, p);
            case HEART -> {
                Summons.spawnAlly(p, Component.translatable("entity.aetherwastes.heart_guardian"), 20 * 45, School.FLOW, School.ASH);
                Summons.spawnAlly(p, Component.translatable("entity.aetherwastes.heart_guardian"), 20 * 45, School.FORGE, School.STAR);
                yield true;
            }
            default -> false;
        };
        if (!done) return InteractionResultHolder.fail(stack);
        if (!p.getAbilities().instabuild) PlayerStats.setVessel(p, PlayerStats.vessel(p) - ability.cost);
        p.getCooldowns().addCooldown(this, ability.cooldown);
        Pulse.addNoise(p, ability.cost * 0.4f);
        stack.hurtAndBreak(1, p, hand == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        return InteractionResultHolder.success(stack);
    }

    private static boolean firebolt(ServerLevel level, ServerPlayer p) {
        Vec3 look = p.getLookAngle();
        for (int i = -1; i <= 1; i++) {
            Vec3 dir = look.yRot(i * 0.12f);
            SmallFireball ball = new SmallFireball(level, p, dir.scale(1.6));
            ball.setPos(p.getX() + look.x, p.getEyeY() - 0.2, p.getZ() + look.z);
            level.addFreshEntity(ball);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 1.2f);
        return true;
    }

    private static boolean slam(ServerLevel level, ServerPlayer p) {
        double r = 5;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(r),
                e -> e != p && e.isAlive() && !SpellEffects.isAlly(p, e) && e.distanceTo(p) <= r)) {
            e.hurt(p.damageSources().playerAttack(p), 9f);
            Vec3 away = e.position().subtract(p.position()).normalize().scale(1.1);
            e.push(away.x, 0.8, away.z);
            e.hurtMarked = true;
        }
        BlockPos below = p.blockPosition().below();
        level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(below)),
                p.getX(), p.getY() + 0.1, p.getZ(), 120, r / 2, 0.1, r / 2, 0.15);
        level.sendParticles(ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.3, p.getZ(), 3, 1.5, 0.1, 1.5, 0);
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 0.7f);
        return true;
    }

    private static boolean blink(ServerLevel level, ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().multiply(1, 0, 1).normalize().scale(9));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation().subtract(p.getLookAngle().scale(0.8));
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, target).inflate(1.2),
                e -> e != p && e.isAlive() && !SpellEffects.isAlly(p, e))) {
            e.hurt(p.damageSources().playerAttack(p), 7f);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2));
        }
        SpellEffects.trail(level, p.position().add(0, 1, 0), target, ParticleTypes.REVERSE_PORTAL);
        p.teleportTo(level, target.x, p.getY(), target.z, p.getYRot(), p.getXRot());
        p.fallDistance = 0;
        level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.4f);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(ability.key()).withStyle(ChatFormatting.GOLD));
        if (ability.cost > 0) {
            tooltip.add(Component.translatable("weapon.aetherwastes.active", ability.cost, ability.cooldown / 20f)
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
