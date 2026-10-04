package com.aetherwastes.item;

import com.aetherwastes.core.Data;
import com.aetherwastes.core.Msg;
import com.aetherwastes.core.PlayerData;
import com.aetherwastes.magic.Glyph;
import com.aetherwastes.network.Sync;
import com.aetherwastes.progression.EraManager;
import com.aetherwastes.progression.School;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Глиф. ПКМ в воздухе глифом-Сутью — изучить школу (если эпоха позволяет). */
public class GlyphItem extends Item {
    private final Glyph glyph;

    public GlyphItem(Glyph glyph, Properties properties) {
        super(properties);
        this.glyph = glyph;
    }

    public Glyph glyph() {
        return glyph;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        School school = glyph.school();
        if (school == null) return InteractionResultHolder.pass(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        ServerPlayer p = (ServerPlayer) player;
        PlayerData d = Data.get(p);
        if (d.knows(school.id())) {
            Msg.bar(p, ChatFormatting.GRAY, "message.aetherwastes.school.known", Component.translatable(school.key()));
            return InteractionResultHolder.fail(stack);
        }
        int max = EraManager.maxSchools(d);
        if (d.schools.size() >= max) {
            Msg.bar(p, ChatFormatting.RED, max == 0 ? "message.aetherwastes.school.no_magic_yet"
                    : "message.aetherwastes.school.limit", max);
            return InteractionResultHolder.fail(stack);
        }
        d.schools.add(school.id());
        d.masteryXp.putIfAbsent(school.id(), 0);
        if (!p.getAbilities().instabuild) stack.shrink(1);
        Msg.title(p, Component.translatable(school.key()).withStyle(school.color()),
                Component.translatable("message.aetherwastes.school.learned"));
        level.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.7f);
        Msg.chat(p, school.color(), "school.aetherwastes." + school.id() + ".desc");
        Sync.journal(p);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(glyph.type().translationKey()).withStyle(glyph.type().color));
        tooltip.add(Component.translatable(glyph.translationKey() + ".desc").withStyle(ChatFormatting.GRAY));
        if (glyph.school() != null) {
            tooltip.add(Component.translatable("tooltip.aetherwastes.glyph.learn").withStyle(ChatFormatting.DARK_AQUA));
        }
        if (glyph.rare()) {
            tooltip.add(Component.translatable("tooltip.aetherwastes.glyph.rare").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return glyph.type() == Glyph.Type.ESSENCE || glyph.rare();
    }
}
