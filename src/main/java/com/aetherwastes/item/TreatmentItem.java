package com.aetherwastes.item;

import com.aetherwastes.core.Msg;
import com.aetherwastes.survival.Traumas;
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

/** Шина, бинт, мазь от ожогов. */
public class TreatmentItem extends Item {
    private final String trauma;

    public TreatmentItem(String trauma, Properties properties) {
        super(properties);
        this.trauma = trauma;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (Traumas.cure((ServerPlayer) player, trauma)) {
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 1.2f);
            return InteractionResultHolder.success(stack);
        }
        Msg.bar(player, ChatFormatting.GRAY, "trauma.aetherwastes.not_needed");
        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.aetherwastes.treats", Component.translatable("trauma.aetherwastes." + trauma))
                .withStyle(ChatFormatting.GRAY));
    }
}
