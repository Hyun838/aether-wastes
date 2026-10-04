package com.aetherwastes.survival;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.ether.EtherField;
import com.aetherwastes.network.Sync;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.ArrayList;
import java.util.List;

/**
 * Мутации — постоянные черты с плюсом и минусом. Появляются после ~10 минут в давлении от 70.
 * Снимаются ритуалом Очищения.
 */
public final class Mutations {
    public static final String GLASS_SKIN = "glass_skin";   // +броня, двойной урон от взрывов
    public static final String NIGHT_EYES = "night_eyes";   // ночное зрение, слабость на солнце
    public static final String ROOTS = "roots";             // регенерация на траве, медленнее бег
    public static final String EMBER_BLOOD = "ember_blood"; // огнестойкость, вода вредит
    public static final String ETHER_LUNGS = "ether_lungs"; // Сосуд +50%, Ясность падает быстрее
    public static final String[] ALL = {GLASS_SKIN, NIGHT_EYES, ROOTS, EMBER_BLOOD, ETHER_LUNGS};

    private static final ResourceLocation ARMOR_ID = AetherWastes.id("mutation_glass_skin");
    private static final ResourceLocation SPEED_ID = AetherWastes.id("mutation_roots");

    private Mutations() {}

    public static void tick(ServerPlayer p, PlayerData d, float pressure) {
        if (AetherConfig.MUTATIONS.get() && !p.isCreative() && pressure >= EtherField.HIGH) {
            d.exposure += pressure >= 85f ? 2f : 1f;
            if (d.exposure >= 600f) {
                d.exposure = 0f;
                gainRandom(p, d);
            }
        } else {
            d.exposure = Math.max(0f, d.exposure - 0.2f);
        }
        apply(p, d);
    }

    public static void gainRandom(ServerPlayer p, PlayerData d) {
        List<String> left = new ArrayList<>();
        for (String m : ALL) if (!d.mutations.contains(m)) left.add(m);
        if (left.isEmpty()) return;
        String m = left.get(p.getRandom().nextInt(left.size()));
        d.mutations.add(m);
        Msg.title(p, Component.translatable("mutation.aetherwastes.title").withStyle(ChatFormatting.DARK_PURPLE),
                Component.translatable("mutation.aetherwastes." + m));
        Msg.chat(p, ChatFormatting.LIGHT_PURPLE, "mutation.aetherwastes." + m + ".desc");
        Sync.journal(p);
    }

    public static String removeLast(ServerPlayer p, PlayerData d) {
        if (d.mutations.isEmpty()) return null;
        String m = d.mutations.iterator().next();
        d.mutations.remove(m);
        apply(p, d);
        Sync.journal(p);
        return m;
    }

    private static void apply(ServerPlayer p, PlayerData d) {
        AttributeInstance armor = p.getAttribute(Attributes.ARMOR);
        if (armor != null) {
            if (d.mutations.contains(GLASS_SKIN)) {
                armor.addOrUpdateTransientModifier(new AttributeModifier(ARMOR_ID, 4.0, AttributeModifier.Operation.ADD_VALUE));
            } else if (armor.hasModifier(ARMOR_ID)) {
                armor.removeModifier(ARMOR_ID);
            }
        }
        AttributeInstance speed = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            if (d.mutations.contains(ROOTS)) {
                speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_ID, -0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else if (speed.hasModifier(SPEED_ID)) {
                speed.removeModifier(SPEED_ID);
            }
        }
        if (d.mutations.isEmpty()) return;

        if (d.mutations.contains(NIGHT_EYES)) {
            p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, true));
            if (p.level().isDay() && p.level().canSeeSky(p.blockPosition().above())) {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
                if (p.getRandom().nextFloat() < 0.05f) p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
            }
        }
        if (d.mutations.contains(ROOTS) && p.level().getBlockState(p.blockPosition().below()).is(BlockTags.DIRT)
                && p.getDeltaMovement().horizontalDistanceSqr() < 0.001) {
            p.heal(0.5f);
        }
        if (d.mutations.contains(EMBER_BLOOD)) {
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
            if (p.isInWaterRainOrBubble()) {
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
                if (p.level().getGameTime() % 100 < 20) p.hurt(p.damageSources().drown(), 1f);
            }
        }
    }
}
