package com.aetherwastes.survival;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.craft.Engraving;
import com.aetherwastes.craft.Traits;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.progression.Insights;
import com.aetherwastes.progression.Mastery;
import com.aetherwastes.progression.School;
import com.aetherwastes.registry.ModBlocks;
import com.aetherwastes.threats.Pulse;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Раз в секунду: Сосуд, Отдача, давление, Ясность, травмы, питание, температура,
 * мутации, ворота эпох, Озарения земель, Пульс Мира, свойства металлов. И синхронизация.
 */
@EventBusSubscriber(modid = AetherWastes.MODID)
public final class SurvivalEvents {
    private static final SoundEvent[] WHISPERS = {
            SoundEvents.CREEPER_PRIMED, SoundEvents.ZOMBIE_AMBIENT, SoundEvents.SKELETON_AMBIENT,
            SoundEvents.SPIDER_AMBIENT, SoundEvents.ENDERMAN_AMBIENT, SoundEvents.WOOD_BREAK
    };

    private SurvivalEvents() {}

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        if (p.isSpectator() || !p.isAlive()) return;
        if (p.tickCount % 20 != 0) return;

        PlayerData d = Data.get(p);
        RandomSource rng = p.getRandom();
        float pressure = EtherField.pressure(p.serverLevel(), p.blockPosition());
        boolean nearAnchor = EtherField.inAnchor(p.serverLevel(), p.blockPosition());
        boolean tide = EtherField.isTide(p.level());

        tickVessel(p, d, pressure, rng);
        tickClarity(p, d, pressure, nearAnchor, rng);
        Traumas.tick(p, d);
        Nutrition.tick(p, d);
        Temperature.tick(p, d);
        Mutations.tick(p, d, pressure);
        Engraving.armorPassives(p);
        EraManager.onTideState(p, tide);
        Pulse.tickPlayer(p, d, pressure, tide);

        if (p.tickCount % 100 == 0) {
            p.level().getBiome(p.blockPosition()).unwrapKey()
                    .ifPresent(k -> Insights.add(p, "land", k.location().toString()));
            Traits.scanInventory(p);
            EraManager.check(p);
            if (p.isScoping() && !p.level().isDay() && p.level().canSeeSky(p.blockPosition().above())) {
                Mastery.add(p, School.STAR, 3);
            }
            if (p.level().getMaxLocalRawBrightness(p.blockPosition()) < 4) Mastery.add(p, School.SILENCE, 1);
        }
        Sync.stats(p);
    }

    private static void tickVessel(ServerPlayer p, PlayerData d, float pressure, RandomSource rng) {
        float vessel = PlayerStats.vessel(p);
        float regen = (pressure / 100f) * 1.5f * AetherConfig.VESSEL_REGEN_MULTIPLIER.get().floatValue();
        if (Nutrition.malnourished(d)) regen *= 0.6f;
        if (d.mutations.contains(Mutations.ETHER_LUNGS)) regen *= 1.5f;
        regen *= 1f + 0.1f * Traits.count(p, Traits.RESONANT);
        float cap = pressure >= 80f ? PlayerStats.VESSEL_OVERCHARGE_MAX : PlayerStats.VESSEL_MAX;
        if (vessel < cap) {
            PlayerStats.setVessel(p, Math.min(cap, vessel + regen));
        } else if (vessel > cap) {
            PlayerStats.setVessel(p, Math.max(cap, vessel - 1f));
        }

        if (p.isCreative() || !AetherConfig.HIGH_PRESSURE_EFFECTS.get()) return;
        if (PlayerStats.vessel(p) > PlayerStats.VESSEL_MAX && rng.nextFloat() < 0.10f) {
            switch (rng.nextInt(4)) {
                case 0 -> p.igniteForSeconds(2f);
                case 1 -> p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
                case 2 -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
                default -> p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
            PlayerStats.setVessel(p, PlayerStats.vessel(p) - 8f);
            Msg.bar(p, ChatFormatting.LIGHT_PURPLE, "message.aetherwastes.vessel.backlash");
        }
        if (pressure >= EtherField.HIGH && rng.nextFloat() < 0.04f) {
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            Msg.bar(p, ChatFormatting.DARK_PURPLE, "message.aetherwastes.pressure.nausea");
        }
    }

    private static void tickClarity(ServerPlayer p, PlayerData d, float pressure, boolean nearAnchor, RandomSource rng) {
        float mult = AetherConfig.CLARITY_DRAIN_MULTIPLIER.get().floatValue();
        if (d.mutations.contains(Mutations.ETHER_LUNGS)) mult *= 1.5f;
        float delta = 0f;
        int light = p.level().getMaxLocalRawBrightness(p.blockPosition());
        if (light < 4 && !nearAnchor) delta -= 0.15f * mult;
        if (pressure >= EtherField.HIGH) delta -= 0.10f * mult;
        if (p.level().getBlockState(p.blockPosition().below()).is(ModBlocks.SALT_CRUST.get())) delta -= 0.08f * mult;
        if (p.level().getBlockState(p.blockPosition().below()).is(ModBlocks.AETHER_SCAR.get())) delta -= 0.2f * mult;
        if (nearAnchor) delta += 0.30f;
        if (p.isSleeping()) delta += 2.0f;
        if (light >= 10 && !nearAnchor && pressure < EtherField.HIGH) delta += 0.03f;
        if (p.isCreative()) delta = Math.max(delta, 0.5f);
        PlayerStats.setClarity(p, PlayerStats.clarity(p) + delta);
        float clarity = PlayerStats.clarity(p);
        if (p.isCreative() || mult <= 0f) return;

        if (clarity < 40f && rng.nextFloat() < 0.12f) {
            SoundEvent s = WHISPERS[rng.nextInt(WHISPERS.length)];
            p.playNotifySound(s, SoundSource.HOSTILE, 0.8f, 0.7f + rng.nextFloat() * 0.5f);
        }
        if (clarity < 15f && AetherConfig.PHANTOM_DAMAGE.get() && rng.nextFloat() < 0.10f) {
            if (d.knows(School.SILENCE.id())) {
                // Безмолвие делает фантомов союзниками.
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false, true));
            } else {
                p.hurt(p.damageSources().magic(), 1.0f);
                Msg.bar(p, ChatFormatting.DARK_RED, "message.aetherwastes.clarity.phantom");
            }
        }
    }

    @SubscribeEvent
    public static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Nutrition.onEaten(p, event.getItem());
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Sync.all(p);
            PlayerData d = Data.get(p);
            if (d.era == 1 && d.insights.isEmpty()) {
                Msg.chat(p, ChatFormatting.LIGHT_PURPLE, "message.aetherwastes.welcome");
            }
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Data.get(p).traumas.clear();
            Data.get(p).bodyTemp = 50f;
            Sync.all(p);
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) Sync.all(p);
    }

    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            var key = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(event.getSmelting().getItem());
            Insights.add(p, "metal", key.toString());
            Mastery.add(p, School.FORGE, Math.max(1, event.getSmelting().getCount()));
        }
    }

}
