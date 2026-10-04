package com.aetherwastes.survival;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.config.AetherConfig;
import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Пять групп пищи: мясо, рыба, овощи, фрукты, зерно. Однообразная еда насыщает хуже;
 * мало разнообразия — Истощение (−4 к здоровью, Сосуд наполняется медленнее).
 */
public final class Nutrition {
    public static final String[] GROUPS = {"meat", "fish", "vegetable", "fruit", "grain"};
    private static final int HISTORY = 15;
    private static final ResourceLocation MALNUTRITION = AetherWastes.id("malnutrition");

    private Nutrition() {}

    public static String groupOf(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (id.contains("beef") || id.contains("pork") || id.contains("chicken") || id.contains("mutton")
                || id.contains("rabbit") || id.contains("flesh") || id.contains("meat")) return "meat";
        if (id.contains("cod") || id.contains("salmon") || id.contains("fish") || id.contains("kelp")) return "fish";
        if (id.contains("carrot") || id.contains("potato") || id.contains("beetroot") || id.contains("mushroom")
                || id.contains("pumpkin")) return "vegetable";
        if (id.contains("apple") || id.contains("berr") || id.contains("melon") || id.contains("chorus")
                || id.contains("fruit")) return "fruit";
        if (id.contains("bread") || id.contains("cookie") || id.contains("cake") || id.contains("pie")
                || id.contains("wheat")) return "grain";
        return null;
    }

    public static void onEaten(ServerPlayer p, ItemStack stack) {
        if (!AetherConfig.NUTRITION.get()) return;
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null) return;
        PlayerData d = Data.get(p);

        if (stack.is(ModItems.HEARTY_STEW.get())) {
            for (String g : GROUPS) push(d, g);
            Msg.bar(p, ChatFormatting.GREEN, "nutrition.aetherwastes.stew");
            return;
        }
        String group = groupOf(stack);
        if (group == null) return;

        int n = d.foodHistory.size();
        boolean monotone = n >= 3 && group.equals(d.foodHistory.get(n - 1))
                && group.equals(d.foodHistory.get(n - 2)) && group.equals(d.foodHistory.get(n - 3));
        if (monotone) {
            // Половина сытости уходит впустую.
            p.getFoodData().addExhaustion(food.nutrition() * 2f);
            Msg.bar(p, ChatFormatting.YELLOW, "nutrition.aetherwastes.monotone",
                    net.minecraft.network.chat.Component.translatable("nutrition.aetherwastes.group." + group));
        }
        push(d, group);
    }

    private static void push(PlayerData d, String group) {
        d.foodHistory.add(group);
        while (d.foodHistory.size() > HISTORY) d.foodHistory.remove(0);
    }

    public static int variety(PlayerData d) {
        Set<String> s = new HashSet<>(d.foodHistory);
        return s.size();
    }

    public static boolean malnourished(PlayerData d) {
        return AetherConfig.NUTRITION.get() && d.foodHistory.size() >= 6 && variety(d) <= 2;
    }

    /** Раз в секунду: применить или снять Истощение. */
    public static void tick(ServerPlayer p, PlayerData d) {
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp == null) return;
        if (malnourished(d)) {
            if (!hp.hasModifier(MALNUTRITION)) {
                hp.addOrUpdateTransientModifier(new AttributeModifier(MALNUTRITION, -4.0, AttributeModifier.Operation.ADD_VALUE));
                Msg.bar(p, ChatFormatting.YELLOW, "nutrition.aetherwastes.malnourished");
            }
            if (p.getHealth() > p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        } else if (hp.hasModifier(MALNUTRITION)) {
            hp.removeModifier(MALNUTRITION);
        }
    }
}
