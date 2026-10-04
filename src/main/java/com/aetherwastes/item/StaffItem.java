package com.aetherwastes.item;

import com.aetherwastes.magic.SpellCaster;
import com.aetherwastes.magic.SpellData;
import com.aetherwastes.registry.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** Эфирный жезл: хранит одно заклинание со Скрижали и произносит его по ПКМ. */
public class StaffItem extends Item {
    public StaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        SpellData spell = stack.get(ModComponents.SPELL.get());
        if (spell == null) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.aetherwastes.staff.empty")
                        .withStyle(ChatFormatting.GRAY), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        boolean cast = SpellCaster.cast((ServerPlayer) player, spell);
        if (cast) {
            player.getCooldowns().addCooldown(this, 12);
            player.swing(hand, true);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return stack.has(ModComponents.SPELL.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SpellData spell = stack.get(ModComponents.SPELL.get());
        if (spell == null) {
            tooltip.add(Component.translatable("tooltip.aetherwastes.staff.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(spell.displayName().withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.aetherwastes.staff.cost", Math.round(spell.baseCost()))
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
