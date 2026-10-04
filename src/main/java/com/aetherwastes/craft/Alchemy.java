package com.aetherwastes.craft;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.progression.Insights;
import com.aetherwastes.survival.PlayerStats;
import com.aetherwastes.survival.Traumas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Алхимия: эффекты трав перемешаны для каждого мира. Узнать их можно, только выпив эликсир.
 */
public final class Alchemy {
    public static final List<Item> HERBS = List.of(
            Items.DANDELION, Items.POPPY, Items.BLUE_ORCHID, Items.ALLIUM, Items.AZURE_BLUET, Items.CORNFLOWER,
            Items.LILY_OF_THE_VALLEY, Items.OXEYE_DAISY, Items.FERN, Items.SWEET_BERRIES, Items.GLOW_BERRIES,
            Items.RED_MUSHROOM, Items.BROWN_MUSHROOM, Items.KELP, Items.SEA_PICKLE, Items.SPORE_BLOSSOM);

    public static final List<String> EFFECTS = List.of(
            "regeneration", "speed", "night_vision", "fire_resistance", "water_breathing", "strength",
            "poison", "weakness", "hunger", "nausea", "slow_falling", "absorption",
            "clarity", "vessel", "mending", "warmth");

    private Alchemy() {}

    public static boolean isHerb(Item item) {
        return HERBS.contains(item);
    }

    public static String herbId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** Эффект травы в этом мире. */
    public static String effectOf(long seed, String herbId) {
        List<String> shuffled = new ArrayList<>(EFFECTS);
        Collections.shuffle(shuffled, new Random(seed * 31 + 0xA1C4E3L));
        for (int i = 0; i < HERBS.size(); i++) {
            if (herbId(HERBS.get(i)).equals(herbId)) return shuffled.get(i % shuffled.size());
        }
        return "regeneration";
    }

    /** Выпить эликсир: применить эффекты и записать открытия в Журнал. */
    public static void drink(ServerPlayer p, List<String> herbs, int strength) {
        long seed = p.serverLevel().getServer().overworld().getSeed();
        PlayerData d = Data.get(p);
        int duration = 20 * 45 * Math.max(1, strength);
        for (String herb : herbs) {
            String effect = effectOf(seed, herb);
            apply(p, effect, duration);
            if (d.knownHerbs.add(herb)) {
                Insights.add(p, "herb", herb);
            }
        }
    }

    private static void apply(ServerPlayer p, String effect, int duration) {
        switch (effect) {
            case "regeneration" -> p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration / 3, 0));
            case "speed" -> p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
            case "night_vision" -> p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, duration * 2, 0));
            case "fire_resistance" -> p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0));
            case "water_breathing" -> p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, duration, 0));
            case "strength" -> p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0));
            case "poison" -> p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            case "weakness" -> p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 0));
            case "hunger" -> p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 400, 0));
            case "nausea" -> p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
            case "slow_falling" -> p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0));
            case "absorption" -> p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0));
            case "clarity" -> PlayerStats.setClarity(p, PlayerStats.clarity(p) + 30f);
            case "vessel" -> PlayerStats.setVessel(p, PlayerStats.vessel(p) + 35f);
            case "mending" -> {
                Traumas.cure(p, Traumas.BLEEDING);
                Traumas.cure(p, Traumas.BURN);
            }
            case "warmth" -> Data.get(p).warmUntil = p.level().getGameTime() + duration;
            default -> {
            }
        }
    }
}
