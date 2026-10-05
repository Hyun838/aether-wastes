package com.aetherwastes.client;

import com.aetherwastes.AetherWastes;
import com.aetherwastes.craft.Alchemy;
import com.aetherwastes.craft.Engraving;
import com.aetherwastes.craft.Quality;
import com.aetherwastes.craft.Traits;
import com.aetherwastes.network.ClientStatsCache;
import com.aetherwastes.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/** Подсказки: качество, свойства материалов, гравировка, травы эликсиров и свойства слитков этого мира. */
@EventBusSubscriber(modid = AetherWastes.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tip = event.getToolTip();

        if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem armor
                && armor.getMaterial().is(com.aetherwastes.registry.ModGear.PHANTOM.getKey())) {
            tip.add(Component.translatable("tooltip.aetherwastes.phantom_set", PhaseClient.PHASE_KEY.getTranslatedKeyMessage())
                    .withStyle(ChatFormatting.AQUA));
        }
        int q = Quality.get(stack);
        if (q >= 0) {
            tip.add(Component.translatable("quality.aetherwastes." + q).withStyle(Quality.color(q)));
        }
        List<String> traits = stack.get(ModComponents.TRAITS.get());
        if (traits != null && !traits.isEmpty()) tip.add(traitLine(traits));

        List<String> eng = stack.get(ModComponents.ENGRAVINGS.get());
        if (stack.isDamageableItem() && (eng != null || q >= 0)) {
            MutableComponent line = Component.translatable("tooltip.aetherwastes.engravings",
                    eng == null ? 0 : eng.size(), Quality.engravingSlots(stack)).withStyle(ChatFormatting.LIGHT_PURPLE);
            if (eng != null) for (String e : eng) line.append(" ").append(Engraving.describe(e));
            tip.add(line);
        }

        List<String> herbs = stack.get(ModComponents.HERBS.get());
        if (herbs != null) {
            CompoundTag known = ClientStatsCache.journal.getCompound("herbs");
            for (String h : herbs) {
                var item = BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(h));
                Component effect = known.contains(h)
                        ? Component.translatable("alchemy.aetherwastes." + known.getString(h)).withStyle(ChatFormatting.GREEN)
                        : Component.literal("???").withStyle(ChatFormatting.DARK_GRAY);
                tip.add(Component.literal(" ").append(item.getDescription()).append(": ").append(effect).withStyle(ChatFormatting.GRAY));
            }
        } else if (Alchemy.isHerb(stack.getItem())) {
            String id = Alchemy.herbId(stack.getItem());
            CompoundTag known = ClientStatsCache.journal.getCompound("herbs");
            if (known.contains(id)) {
                tip.add(Component.translatable("tooltip.aetherwastes.herb_effect",
                        Component.translatable("alchemy.aetherwastes." + known.getString(id))).withStyle(ChatFormatting.DARK_GREEN));
            }
        }

        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String metal : Traits.METALS) {
            if (path.equals(metal + "_ingot") || (metal.equals("star_iron") && path.equals("star_iron_ingot"))) {
                ListTag list = ClientStatsCache.journal.getCompound("metals").getList(metal, Tag.TAG_STRING);
                if (!list.isEmpty()) {
                    MutableComponent line = Component.translatable("tooltip.aetherwastes.metal_traits").withStyle(ChatFormatting.DARK_AQUA);
                    for (int i = 0; i < list.size(); i++) {
                        line.append(" ").append(Component.translatable("trait.aetherwastes." + list.getString(i)));
                    }
                    tip.add(line);
                }
            }
        }
    }

    private static Component traitLine(List<String> traits) {
        MutableComponent line = Component.translatable("tooltip.aetherwastes.traits").withStyle(ChatFormatting.DARK_AQUA);
        for (String t : traits) line.append(" ").append(Component.translatable("trait.aetherwastes." + t));
        return line;
    }
}
